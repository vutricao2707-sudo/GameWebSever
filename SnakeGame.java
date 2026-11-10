import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {

    private static final int CELL = 25;          // kích thước mỗi ô (px)
    private static final int COLS = 24;          // số cột
    private static final int ROWS = 24;          // số hàng
    private static final int WIDTH = COLS * CELL;
    private static final int HEIGHT = ROWS * CELL;
    private static final int START_DELAY = 140;  // ms giữa các bước (càng nhỏ càng nhanh)
    private static final int MIN_DELAY = 60;

    private enum Dir { UP, DOWN, LEFT, RIGHT }

    private final LinkedList<Point> snake = new LinkedList<>();
    private final Deque<Dir> inputQueue = new ArrayDeque<>();
    private final Random random = new Random();
    private final Timer timer;

    private Dir direction;
    private Point food;
    private int score;
    private int highScore;
    private boolean running;
    private boolean paused;
    private boolean gameOver;

    public SnakeGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(24, 26, 32));
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(START_DELAY, this);
        startGame();
    }

    private void startGame() {
        snake.clear();
        inputQueue.clear();
        int startX = COLS / 2;
        int startY = ROWS / 2;
        for (int i = 0; i < 3; i++) {
            snake.add(new Point(startX - i, startY));
        }
        direction = Dir.RIGHT;
        score = 0;
        running = true;
        paused = false;
        gameOver = false;
        timer.setDelay(START_DELAY);
        spawnFood();
        timer.start();
        repaint();
    }

    private void spawnFood() {
        Point p;
        do {
            p = new Point(random.nextInt(COLS), random.nextInt(ROWS));
        } while (snake.contains(p));
        food = p;
    }

    private void step() {
        // Lấy hướng đi tiếp theo từ hàng đợi phím (tránh quay đầu 180 độ)
        if (!inputQueue.isEmpty()) {
            Dir next = inputQueue.poll();
            if (!isOpposite(next, direction)) {
                direction = next;
            }
        }

        Point head = snake.getFirst();
        Point newHead = new Point(head);
        switch (direction) {
            case UP:    newHead.y--; break;
            case DOWN:  newHead.y++; break;
            case LEFT:  newHead.x--; break;
            case RIGHT: newHead.x++; break;
        }

        // Va vào tường
        if (newHead.x < 0 || newHead.x >= COLS || newHead.y < 0 || newHead.y >= ROWS) {
            endGame();
            return;
        }

        boolean ate = newHead.equals(food);

        // Va vào thân (đuôi sẽ di chuyển đi nên không tính nếu không ăn mồi)
        int checkLen = ate ? snake.size() : snake.size() - 1;
        for (int i = 0; i < checkLen; i++) {
            if (snake.get(i).equals(newHead)) {
                endGame();
                return;
            }
        }

        snake.addFirst(newHead);
        if (ate) {
            score += 10;
            if (score > highScore) highScore = score;
            // Tăng tốc dần
            int delay = Math.max(MIN_DELAY, START_DELAY - (score / 10) * 3);
            timer.setDelay(delay);
            if (snake.size() == COLS * ROWS) { // thắng khi lấp đầy bản đồ
                endGame();
                return;
            }
            spawnFood();
        } else {
            snake.removeLast();
        }
    }

    private void endGame() {
        running = false;
        gameOver = true;
        timer.stop();
        if (score > highScore) highScore = score;
        repaint();
    }

    private boolean isOpposite(Dir a, Dir b) {
        return (a == Dir.UP && b == Dir.DOWN) || (a == Dir.DOWN && b == Dir.UP)
                || (a == Dir.LEFT && b == Dir.RIGHT) || (a == Dir.RIGHT && b == Dir.LEFT);
    }

    // ===================== Vẽ =====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Lưới caro mờ
        g2.setColor(new Color(32, 35, 43));
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if ((x + y) % 2 == 0) {
                    g2.fillRect(x * CELL, y * CELL, CELL, CELL);
                }
            }
        }

        // Mồi
        if (food != null) {
            g2.setColor(new Color(239, 83, 80));
            g2.fillOval(food.x * CELL + 3, food.y * CELL + 3, CELL - 6, CELL - 6);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(food.x * CELL + 7, food.y * CELL + 6, 6, 6);
        }

        // Rắn
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            if (i == 0) {
                g2.setColor(new Color(129, 199, 132));
                g2.fillRoundRect(p.x * CELL + 1, p.y * CELL + 1, CELL - 2, CELL - 2, 10, 10);
                drawEyes(g2, p);
            } else {
                // Màu thân đậm dần về phía đuôi
                float t = (float) i / snake.size();
                int green = (int) (170 - 70 * t);
                g2.setColor(new Color(56, green, 80));
                g2.fillRoundRect(p.x * CELL + 2, p.y * CELL + 2, CELL - 4, CELL - 4, 8, 8);
            }
        }

        // Điểm số
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("Điểm: " + score, 10, 22);
        String hs = "Cao nhất: " + highScore;
        g2.drawString(hs, WIDTH - g2.getFontMetrics().stringWidth(hs) - 10, 22);

        if (paused) {
            drawCenteredText(g2, "TẠM DỪNG", "Nhấn P để tiếp tục");
        } else if (gameOver) {
            drawCenteredText(g2, "GAME OVER", "Điểm của bạn: " + score + "  |  Nhấn Enter để chơi lại");
        }
    }

    private void drawEyes(Graphics2D g2, Point p) {
        int cx = p.x * CELL;
        int cy = p.y * CELL;
        int e1x, e1y, e2x, e2y;
        switch (direction) {
            case UP:    e1x = cx + 6;  e1y = cy + 6;  e2x = cx + 15; e2y = cy + 6;  break;
            case DOWN:  e1x = cx + 6;  e1y = cy + 15; e2x = cx + 15; e2y = cy + 15; break;
            case LEFT:  e1x = cx + 6;  e1y = cy + 6;  e2x = cx + 6;  e2y = cy + 15; break;
            default:    e1x = cx + 15; e1y = cy + 6;  e2x = cx + 15; e2y = cy + 15; break;
        }
        g2.setColor(Color.WHITE);
        g2.fillOval(e1x, e1y, 5, 5);
        g2.fillOval(e2x, e2y, 5, 5);
        g2.setColor(Color.BLACK);
        g2.fillOval(e1x + 1, e1y + 1, 2, 2);
        g2.fillOval(e2x + 1, e2y + 1, 2, 2);
    }

    private void drawCenteredText(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 44));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 2 - 10);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        g2.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, HEIGHT / 2 + 25);
    }

    // ===================== Sự kiện =====================

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            step();
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameOver) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                startGame();
            }
            return;
        }

        if (key == KeyEvent.VK_P) {
            paused = !paused;
            repaint();
            return;
        }

        Dir wanted = null;
        switch (key) {
            case KeyEvent.VK_UP:    case KeyEvent.VK_W: wanted = Dir.UP;    break;
            case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: wanted = Dir.DOWN;  break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: wanted = Dir.LEFT;  break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: wanted = Dir.RIGHT; break;
            default: break;
        }
        if (wanted != null && inputQueue.size() < 3) {
            Dir last = inputQueue.isEmpty() ? direction : inputQueue.peekLast();
            if (wanted != last && !isOpposite(wanted, last)) {
                inputQueue.offer(wanted);
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }

    // ===================== Main =====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rắn săn mồi");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new SnakeGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}