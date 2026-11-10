import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BlockBlast extends JPanel {

    private static final int N = 8;            // kích thước bảng N x N
    private static final int CELL = 48;        // kích thước ô trên bảng
    private static final int TRAY_CELL = 24;   // kích thước ô khi khối nằm trong khay
    private static final int BX = 30;          // vị trí bảng
    private static final int BY = 90;
    private static final int BOARD = N * CELL;
    private static final int TRAY_Y = BY + BOARD + 25;
    private static final int SLOT_W = BOARD / 3;
    private static final int SLOT_H = 135;
    private static final int LIFT = 70;        // nâng khối lên khi kéo để không bị ngón tay/chuột che

    private static final Color BG = new Color(30, 34, 52);
    private static final Color BOARD_BG = new Color(44, 50, 78);
    private static final Color CELL_EMPTY = new Color(56, 63, 96);
    private static final Color[] PALETTE = {
            new Color(239, 83, 80), new Color(255, 167, 38), new Color(255, 214, 64),
            new Color(102, 187, 106), new Color(66, 165, 245), new Color(171, 107, 232),
            new Color(236, 100, 170)
    };

    // ===================== Khối =====================

    private static class Piece {
        final boolean[][] shape;
        final int h, w, cells;
        final Color color;

        Piece(boolean[][] shape, Color color) {
            this.shape = shape;
            this.h = shape.length;
            this.w = shape[0].length;
            this.color = color;
            int c = 0;
            for (boolean[] row : shape) for (boolean b : row) if (b) c++;
            this.cells = c;
        }
    }

    private static final List<boolean[][]> SHAPES = new ArrayList<>();

    static {
        // Mỗi mảng là một khối cơ bản; các phép xoay sẽ được thêm tự động
        String[][] base = {
                {"#"},
                {"##"},
                {"###"},
                {"####"},
                {"#####"},
                {"##", "##"},
                {"###", "###", "###"},
                {"###", "###"},
                {"#.", "##"},
                {"#..", "#..", "###"},
                {"#.", "#.", "##"},
                {"###", ".#."},
                {".##", "##."},
                {"##.", ".##"},
                {"#..", "###"},
                {"##", "#.", "#."},
        };
        Set<String> seen = new HashSet<>();
        for (String[] b : base) {
            boolean[][] s = parse(b);
            for (int r = 0; r < 4; r++) {
                String key = key(s);
                if (seen.add(key)) SHAPES.add(s);
                s = rotate(s);
            }
        }
    }

    private static boolean[][] parse(String[] rows) {
        boolean[][] s = new boolean[rows.length][rows[0].length()];
        for (int i = 0; i < rows.length; i++)
            for (int j = 0; j < rows[i].length(); j++)
                s[i][j] = rows[i].charAt(j) == '#';
        return s;
    }

    private static boolean[][] rotate(boolean[][] s) {
        int h = s.length, w = s[0].length;
        boolean[][] r = new boolean[w][h];
        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++)
                r[j][h - 1 - i] = s[i][j];
        return r;
    }

    private static String key(boolean[][] s) {
        StringBuilder sb = new StringBuilder();
        for (boolean[] row : s) {
            for (boolean b : row) sb.append(b ? '#' : '.');
            sb.append('/');
        }
        return sb.toString();
    }

    // ===================== Trạng thái game =====================

    private final Random random = new Random();
    private final Color[][] grid = new Color[N][N];
    private final Piece[] tray = new Piece[3];

    private int score;
    private int highScore;
    private int combo;          // số lượt đặt liên tiếp có xóa hàng/cột
    private boolean gameOver;

    private int dragIndex = -1;
    private int mouseX, mouseY;
    private String message = "";
    private long messageUntil;

    // Hiệu ứng nháy khi xóa hàng/cột
    private final boolean[][] flash = new boolean[N][N];
    private final javax.swing.Timer flashTimer;

    public BlockBlast() {
        setPreferredSize(new Dimension(BX * 2 + BOARD, TRAY_Y + SLOT_H + 20));
        setBackground(BG);
        setFocusable(true);

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onPress(e.getX(), e.getY()); }
            @Override public void mouseDragged(MouseEvent e) { onDrag(e.getX(), e.getY()); }
            @Override public void mouseReleased(MouseEvent e) { onRelease(e.getX(), e.getY()); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (gameOver && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
                    newGame();
                }
            }
        });

        flashTimer = new javax.swing.Timer(220, e -> {
            for (boolean[] row : flash) java.util.Arrays.fill(row, false);
            ((javax.swing.Timer) e.getSource()).stop();
            repaint();
        });
        flashTimer.setRepeats(false);

        newGame();
    }

    private void newGame() {
        for (Color[] row : grid) java.util.Arrays.fill(row, null);
        score = 0;
        combo = 0;
        gameOver = false;
        dragIndex = -1;
        message = "";
        refillTray();
        repaint();
    }

    private void refillTray() {
        // Thử vài lần để đảm bảo bộ 3 khối mới có ít nhất 1 khối đặt được
        for (int attempt = 0; attempt < 30; attempt++) {
            for (int i = 0; i < 3; i++) {
                boolean[][] s = SHAPES.get(random.nextInt(SHAPES.size()));
                tray[i] = new Piece(s, PALETTE[random.nextInt(PALETTE.length)]);
            }
            for (Piece p : tray) {
                if (canPlaceAnywhere(p)) return;
            }
        }
    }

    // ===================== Logic =====================

    private boolean fits(Piece p, int gx, int gy) {
        for (int i = 0; i < p.h; i++) {
            for (int j = 0; j < p.w; j++) {
                if (!p.shape[i][j]) continue;
                int r = gy + i, c = gx + j;
                if (r < 0 || r >= N || c < 0 || c >= N) return false;
                if (grid[r][c] != null) return false;
            }
        }
        return true;
    }

    private boolean canPlaceAnywhere(Piece p) {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (fits(p, c, r)) return true;
        return false;
    }

    private void place(Piece p, int gx, int gy) {
        for (int i = 0; i < p.h; i++)
            for (int j = 0; j < p.w; j++)
                if (p.shape[i][j]) grid[gy + i][gx + j] = p.color;

        int gained = p.cells;

        // Tìm các hàng/cột đã đầy
        boolean[] fullRow = new boolean[N];
        boolean[] fullCol = new boolean[N];
        int lines = 0;
        for (int r = 0; r < N; r++) {
            boolean full = true;
            for (int c = 0; c < N; c++) if (grid[r][c] == null) { full = false; break; }
            fullRow[r] = full;
            if (full) lines++;
        }
        for (int c = 0; c < N; c++) {
            boolean full = true;
            for (int r = 0; r < N; r++) if (grid[r][c] == null) { full = false; break; }
            fullCol[c] = full;
            if (full) lines++;
        }

        if (lines > 0) {
            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++)
                    if (fullRow[r] || fullCol[c]) {
                        flash[r][c] = true;
                        grid[r][c] = null;
                    }
            combo++;
            gained += lines * lines * 10 + (combo - 1) * 10;
            message = lines > 1 ? "x" + lines + " hàng/cột!" : "Xóa hàng!";
            if (combo > 1) message += "  Combo " + combo;
            messageUntil = System.currentTimeMillis() + 1500;
            flashTimer.restart();
        } else {
            combo = 0;
        }

        score += gained;
        if (score > highScore) highScore = score;
    }

    private void afterPlacement() {
        boolean empty = true;
        for (Piece p : tray) if (p != null) { empty = false; break; }
        if (empty) refillTray();

        // Kiểm tra thua: không còn khối nào đặt vừa
        boolean anyFits = false;
        for (Piece p : tray) {
            if (p != null && canPlaceAnywhere(p)) { anyFits = true; break; }
        }
        if (!anyFits) gameOver = true;
    }

    // ===================== Chuột =====================

    private Rectangle slotRect(int i) {
        return new Rectangle(BX + i * SLOT_W, TRAY_Y, SLOT_W, SLOT_H);
    }

    private void onPress(int x, int y) {
        requestFocusInWindow();
        if (gameOver) {
            newGame();
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (tray[i] != null && slotRect(i).contains(x, y)) {
                dragIndex = i;
                mouseX = x;
                mouseY = y;
                repaint();
                return;
            }
        }
    }

    private void onDrag(int x, int y) {
        if (dragIndex < 0) return;
        mouseX = x;
        mouseY = y;
        repaint();
    }

    private void onRelease(int x, int y) {
        if (dragIndex < 0) return;
        mouseX = x;
        mouseY = y;
        Piece p = tray[dragIndex];
        int[] g = snap(p);
        if (g != null && fits(p, g[0], g[1])) {
            place(p, g[0], g[1]);
            tray[dragIndex] = null;
            dragIndex = -1;
            afterPlacement();
        } else {
            dragIndex = -1;
        }
        repaint();
    }

    /** Vị trí góc trên trái (theo pixel) của khối đang kéo. */
    private Point dragOrigin(Piece p) {
        return new Point(mouseX - p.w * CELL / 2, mouseY - p.h * CELL / 2 - LIFT);
    }

    /** Ô lưới gần nhất mà khối đang kéo sẽ rơi vào. */
    private int[] snap(Piece p) {
        Point o = dragOrigin(p);
        int gx = Math.round((o.x - BX) / (float) CELL);
        int gy = Math.round((o.y - BY) / (float) CELL);
        return new int[]{gx, gy};
    }

    // ===================== Vẽ =====================

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawHeader(g);

        // Nền bảng
        g.setColor(BOARD_BG);
        g.fillRoundRect(BX - 6, BY - 6, BOARD + 12, BOARD + 12, 18, 18);

        // Gợi ý (ghost) khi đang kéo
        boolean[][] ghost = new boolean[N][N];
        boolean[] previewRow = new boolean[N];
        boolean[] previewCol = new boolean[N];
        if (dragIndex >= 0 && tray[dragIndex] != null) {
            Piece p = tray[dragIndex];
            int[] s = snap(p);
            if (fits(p, s[0], s[1])) {
                for (int i = 0; i < p.h; i++)
                    for (int j = 0; j < p.w; j++)
                        if (p.shape[i][j]) ghost[s[1] + i][s[0] + j] = true;
                computePreviewLines(ghost, previewRow, previewCol);
            }
        }

        // Các ô trên bảng
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                int x = BX + c * CELL, y = BY + r * CELL;
                if (grid[r][c] != null) {
                    drawBlock(g, x, y, CELL, grid[r][c]);
                } else {
                    g.setColor(CELL_EMPTY);
                    g.fillRoundRect(x + 2, y + 2, CELL - 4, CELL - 4, 8, 8);
                }
                if (ghost[r][c]) {
                    Color pc = tray[dragIndex].color;
                    g.setColor(new Color(pc.getRed(), pc.getGreen(), pc.getBlue(), 120));
                    g.fillRoundRect(x + 2, y + 2, CELL - 4, CELL - 4, 8, 8);
                } else if (grid[r][c] == null && (previewRow[r] || previewCol[c])) {
                    g.setColor(new Color(255, 255, 255, 40));
                    g.fillRoundRect(x + 2, y + 2, CELL - 4, CELL - 4, 8, 8);
                }
                if (flash[r][c]) {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fillRoundRect(x + 2, y + 2, CELL - 4, CELL - 4, 8, 8);
                }
            }
        }

        // Khay chứa khối
        for (int i = 0; i < 3; i++) {
            Piece p = tray[i];
            if (p == null || i == dragIndex) continue;
            Rectangle sr = slotRect(i);
            int ox = sr.x + (sr.width - p.w * TRAY_CELL) / 2;
            int oy = sr.y + (sr.height - p.h * TRAY_CELL) / 2;
            boolean playable = canPlaceAnywhere(p);
            drawPiece(g, p, ox, oy, TRAY_CELL, playable ? 255 : 90);
        }

        // Khối đang kéo (vẽ ở kích thước thật)
        if (dragIndex >= 0 && tray[dragIndex] != null) {
            Piece p = tray[dragIndex];
            Point o = dragOrigin(p);
            drawPiece(g, p, o.x, o.y, CELL, 230);
        }

        // Thông báo combo
        if (System.currentTimeMillis() < messageUntil && !message.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            FontMetrics fm = g.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(message)) / 2;
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(message, tx + 2, BY + BOARD / 2 + 2);
            g.setColor(new Color(255, 235, 120));
            g.drawString(message, tx, BY + BOARD / 2);
            repaintLater();
        }

        if (gameOver) drawGameOver(g);
    }

    private void repaintLater() {
        javax.swing.Timer t = new javax.swing.Timer(1600, e -> repaint());
        t.setRepeats(false);
        t.start();
    }

    /** Đánh dấu các hàng/cột sẽ được xóa nếu đặt khối vào vị trí ghost. */
    private void computePreviewLines(boolean[][] ghost, boolean[] rows, boolean[] cols) {
        for (int r = 0; r < N; r++) {
            boolean full = true;
            for (int c = 0; c < N; c++) if (grid[r][c] == null && !ghost[r][c]) { full = false; break; }
            rows[r] = full;
        }
        for (int c = 0; c < N; c++) {
            boolean full = true;
            for (int r = 0; r < N; r++) if (grid[r][c] == null && !ghost[r][c]) { full = false; break; }
            cols[c] = full;
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("ĐIỂM", BX, 32);
        g.drawString("CAO NHẤT", BX + BOARD - g.getFontMetrics().stringWidth("CAO NHẤT"), 32);

        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString(String.valueOf(score), BX, 68);
        String hs = String.valueOf(highScore);
        g.setColor(new Color(255, 214, 64));
        g.drawString(hs, BX + BOARD - g.getFontMetrics().stringWidth(hs), 68);
    }

    private void drawPiece(Graphics2D g, Piece p, int ox, int oy, int size, int alpha) {
        Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha);
        for (int i = 0; i < p.h; i++)
            for (int j = 0; j < p.w; j++)
                if (p.shape[i][j]) drawBlock(g, ox + j * size, oy + i * size, size, c);
    }

    private void drawBlock(Graphics2D g, int x, int y, int size, Color c) {
        int pad = Math.max(1, size / 24);
        g.setColor(c);
        g.fillRoundRect(x + pad, y + pad, size - 2 * pad, size - 2 * pad, size / 4, size / 4);
        // Viền sáng phía trên để tạo cảm giác nổi khối
        g.setColor(new Color(255, 255, 255, Math.min(90, c.getAlpha() / 3)));
        g.fillRoundRect(x + pad + size / 8, y + pad + size / 10, size - 2 * pad - size / 4, size / 5, size / 6, size / 6);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);

        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        String t = "HẾT CHỖ!";
        g.drawString(t, (getWidth() - g.getFontMetrics().stringWidth(t)) / 2, getHeight() / 2 - 30);

        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        String s = "Điểm: " + score + "   |   Cao nhất: " + highScore;
        g.drawString(s, (getWidth() - g.getFontMetrics().stringWidth(s)) / 2, getHeight() / 2 + 10);

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String r = "Nhấn chuột hoặc Enter để chơi lại";
        g.drawString(r, (getWidth() - g.getFontMetrics().stringWidth(r)) / 2, getHeight() / 2 + 45);
    }

    // ===================== Main =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Block Blast - Xếp hình");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new BlockBlast());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}