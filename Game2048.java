import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game2048 extends JPanel {

    private static final int N = 4;
    private static final int TILE = 100;
    private static final int GAP = 12;
    private static final int BX = 20;
    private static final int BY = 130;
    private static final int BOARD = N * TILE + (N + 1) * GAP;

    private static final Color BG = new Color(250, 248, 239);
    private static final Color BOARD_BG = new Color(187, 173, 160);
    private static final Color EMPTY = new Color(205, 193, 180);

    private enum Dir { UP, DOWN, LEFT, RIGHT }

    private final Random random = new Random();
    private int[][] board = new int[N][N];
    private int[][] prevBoard;
    private int prevScore;

    private int score;
    private int best;
    private boolean won;          // đã đạt 2048
    private boolean keepPlaying;  // tiếp tục sau khi thắng
    private boolean gameOver;

    private int pressX, pressY;

    public Game2048() {
        setPreferredSize(new Dimension(BOARD + BX * 2, BY + BOARD + 60));
        setBackground(BG);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    case KeyEvent.VK_W: move(Dir.UP);    break;
                    case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: move(Dir.DOWN);  break;
                    case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: move(Dir.LEFT);  break;
                    case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: move(Dir.RIGHT); break;
                    case KeyEvent.VK_U: undo(); break;
                    case KeyEvent.VK_R: case KeyEvent.VK_ENTER: newGame(); break;
                    case KeyEvent.VK_SPACE:
                        if (won && !keepPlaying) { keepPlaying = true; repaint(); }
                        break;
                    default: break;
                }
            }
        });

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pressX = e.getX();
                pressY = e.getY();
            }
            @Override public void mouseReleased(MouseEvent e) {
                int dx = e.getX() - pressX;
                int dy = e.getY() - pressY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) < 30) return; // kéo quá ngắn
                if (Math.abs(dx) > Math.abs(dy)) move(dx > 0 ? Dir.RIGHT : Dir.LEFT);
                else move(dy > 0 ? Dir.DOWN : Dir.UP);
            }
        };
        addMouseListener(ma);

        newGame();
    }

    // ===================== Logic =====================

    private void newGame() {
        board = new int[N][N];
        prevBoard = null;
        score = 0;
        won = false;
        keepPlaying = false;
        gameOver = false;
        addRandomTile();
        addRandomTile();
        repaint();
    }

    private void addRandomTile() {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (board[r][c] == 0) empty.add(new int[]{r, c});
        if (empty.isEmpty()) return;
        int[] cell = empty.get(random.nextInt(empty.size()));
        board[cell[0]][cell[1]] = random.nextInt(10) == 0 ? 4 : 2; // 90% ra 2, 10% ra 4
    }

    /** Dồn một hàng về phía đầu mảng và gộp các ô bằng nhau. Trả về điểm nhận được ở phần tử cuối. */
    private int[] slide(int[] line) {
        int[] out = new int[N + 1]; // out[N] = điểm
        int idx = 0;
        int last = 0;
        for (int v : line) {
            if (v == 0) continue;
            if (last != 0 && last == v) {
                out[idx - 1] = v * 2;
                out[N] += v * 2;
                last = 0; // ô vừa gộp không được gộp tiếp trong cùng lượt
            } else {
                out[idx++] = v;
                last = v;
            }
        }
        return out;
    }

    private void move(Dir dir) {
        if (gameOver || (won && !keepPlaying)) return;

        int[][] snapshot = copy(board);
        int gainedTotal = 0;
        boolean moved = false;

        for (int i = 0; i < N; i++) {
            int[] line = new int[N];
            for (int k = 0; k < N; k++) {
                int[] p = pos(dir, i, k);
                line[k] = board[p[0]][p[1]];
            }
            int[] res = slide(line);
            gainedTotal += res[N];
            for (int k = 0; k < N; k++) {
                int[] p = pos(dir, i, k);
                if (board[p[0]][p[1]] != res[k]) moved = true;
                board[p[0]][p[1]] = res[k];
            }
        }

        if (!moved) return;

        prevBoard = snapshot;
        prevScore = score;
        score += gainedTotal;
        if (score > best) best = score;
        addRandomTile();

        if (!won) {
            for (int[] row : board)
                for (int v : row)
                    if (v >= 2048) won = true;
        }
        if (!canMove()) gameOver = true;
        repaint();
    }

    /** Tọa độ ô thứ k của hàng/cột thứ i theo hướng lướt (k = 0 là ô nằm sát cạnh đích). */
    private int[] pos(Dir dir, int i, int k) {
        switch (dir) {
            case LEFT:  return new int[]{i, k};
            case RIGHT: return new int[]{i, N - 1 - k};
            case UP:    return new int[]{k, i};
            default:    return new int[]{N - 1 - k, i};
        }
    }

    private boolean canMove() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (board[r][c] == 0) return true;
                if (c + 1 < N && board[r][c] == board[r][c + 1]) return true;
                if (r + 1 < N && board[r][c] == board[r + 1][c]) return true;
            }
        }
        return false;
    }

    private void undo() {
        if (prevBoard == null) return;
        board = prevBoard;
        score = prevScore;
        prevBoard = null;
        gameOver = false;
        repaint();
    }

    private int[][] copy(int[][] src) {
        int[][] d = new int[N][];
        for (int i = 0; i < N; i++) d[i] = src[i].clone();
        return d;
    }

    // ===================== Vẽ =====================

    private Color tileColor(int v) {
        switch (v) {
            case 2:    return new Color(238, 228, 218);
            case 4:    return new Color(237, 224, 200);
            case 8:    return new Color(242, 177, 121);
            case 16:   return new Color(245, 149, 99);
            case 32:   return new Color(246, 124, 95);
            case 64:   return new Color(246, 94, 59);
            case 128:  return new Color(237, 207, 114);
            case 256:  return new Color(237, 204, 97);
            case 512:  return new Color(237, 200, 80);
            case 1024: return new Color(237, 197, 63);
            case 2048: return new Color(237, 194, 46);
            default:   return new Color(60, 58, 50);
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawHeader(g);

        // Bảng
        g.setColor(BOARD_BG);
        g.fillRoundRect(BX, BY, BOARD, BOARD, 16, 16);

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                int x = BX + GAP + c * (TILE + GAP);
                int y = BY + GAP + r * (TILE + GAP);
                int v = board[r][c];

                g.setColor(v == 0 ? EMPTY : tileColor(v));
                g.fillRoundRect(x, y, TILE, TILE, 10, 10);

                if (v != 0) {
                    g.setColor(v <= 4 ? new Color(119, 110, 101) : Color.WHITE);
                    int size = v < 100 ? 44 : v < 1000 ? 38 : v < 10000 ? 32 : 26;
                    g.setFont(new Font("SansSerif", Font.BOLD, size));
                    FontMetrics fm = g.getFontMetrics();
                    String s = String.valueOf(v);
                    g.drawString(s,
                            x + (TILE - fm.stringWidth(s)) / 2,
                            y + (TILE - fm.getHeight()) / 2 + fm.getAscent());
                }
            }
        }

        // Hướng dẫn
        g.setColor(new Color(119, 110, 101));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Mũi tên/WASD hoặc lướt chuột để di chuyển  |  U: hoàn tác  |  R: chơi lại",
                BX, BY + BOARD + 30);

        if (gameOver) {
            drawOverlay(g, "HẾT NƯỚC ĐI!", "Điểm: " + score + "  -  Nhấn R để chơi lại",
                    new Color(238, 228, 218, 200), new Color(119, 110, 101));
        } else if (won && !keepPlaying) {
            drawOverlay(g, "BẠN THẮNG!", "Space: chơi tiếp  -  R: chơi lại",
                    new Color(237, 194, 46, 190), Color.WHITE);
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(new Color(119, 110, 101));
        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        g.drawString("2048", BX, 75);

        drawScoreBox(g, "ĐIỂM", score, BX + BOARD - 2 * 100 - 10, 25);
        drawScoreBox(g, "CAO NHẤT", best, BX + BOARD - 100, 25);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(119, 110, 101));
        g.drawString("Ghép các ô giống nhau để đạt ô 2048!", BX, 108);
    }

    private void drawScoreBox(Graphics2D g, String label, int value, int x, int y) {
        int w = 100, h = 60;
        g.setColor(BOARD_BG);
        g.fillRoundRect(x, y, w, h, 8, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(238, 228, 218));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + 20);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        fm = g.getFontMetrics();
        String s = String.valueOf(value);
        g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + 47);
    }

    private void drawOverlay(Graphics2D g, String title, String sub, Color bg, Color fg) {
        g.setColor(bg);
        g.fillRoundRect(BX, BY, BOARD, BOARD, 16, 16);
        g.setColor(fg);

        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, BX + (BOARD - fm.stringWidth(title)) / 2, BY + BOARD / 2 - 5);

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        fm = g.getFontMetrics();
        g.drawString(sub, BX + (BOARD - fm.stringWidth(sub)) / 2, BY + BOARD / 2 + 30);
    }

    // ===================== Main =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("2048");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new Game2048());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}