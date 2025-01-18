package pro;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;
/**
 * PianoMemoryGame
 * 게임에서 사용자가 피아노 건반의 연주 순서를 기억하고
 * 동일한 순서로 클릭하여 점수를 획득하는 기억력 테스트 게임.
 */
public class PianoMemoryGame extends JFrame {
    // 게임 상태를 관리하는 필드
    private ArrayList<Integer> sequence = new ArrayList<>();// 컴퓨터가 생성한 연주 순서
    private ArrayList<Integer> userInput = new ArrayList<>();//사용자가 입력한 순서
    private int level = 1;// 현재 레벨
    private int score = 0;// 현재 점수
    private int highScore = 0; // 최고 점수
    private String playerId = "플레이어"; // 현재 플레이어 ID
    private String highScorePlayer = "플레이어"; // 최고 점수를 기록한 플레이어 ID
    

    // GUI 컴포넌트
    private JLabel levelLabel, scoreLabel, highScoreLabel;// 점수와 레벨 표시용 레이블
    private JButton[] buttons = new JButton[8]; // 피아노 건반 버튼
    private Random random = new Random(); // 순서 생성을 위한 랜덤 객체
    private Timer inputTimer;// 입력 시간 제한 타이머
    private final int INPUT_TIME_LIMIT = 5000;  // 입력 제한 시간 (5초)
    
    // 피아노 건반 정보 및 사운드 파일
    private String[] pianoKeys = {"도", "레", "미", "파", "솔", "라", "시", "도"};
    private String[] buttonSounds = {"sounds/도.wav", "sounds/레.wav", "sounds/미.wav", "sounds/파.wav", "sounds/솔.wav", "sounds/라.wav", "sounds/시.wav", "sounds/도높은.wav"};
    private boolean isGameEnded = false;

    
    // 음계별 강조 색상(테마별로 나눔)
    
    private Color[] defaultHighlightColors = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.PINK
    };
    private Color[] darkHighlightColors = {
        new Color(33, 33, 33),   // 매우 어두운 회색
        new Color(85, 85, 85),   // 짙은 회색
        new Color(0, 51, 102),   // 짙은 파랑
        new Color(0, 102, 51),   // 짙은 녹색
        new Color(51, 0, 102),   // 짙은 보라색
        new Color(0, 51, 51),    // 짙은 청록색
        new Color(102, 51, 0),   // 짙은 갈색
        new Color(51, 0, 0)      // 짙은 적갈색
    };
    private Color[] brightHighlightColors = {
        new Color(255, 182, 193),  // 라이트 핑크
        new Color(255, 239, 213),  // 페일 골드
        new Color(240, 255, 240),  // 허니듀
        new Color(173, 216, 230),  // 라이트 블루
        new Color(224, 255, 255),  // 라이트 시안
        new Color(250, 250, 210),  // 라이트 옐로우
        new Color(255, 228, 225),  // 미스트 로즈
        new Color(255, 240, 245)   // 라벤더 블러쉬
    };
    private Color[] currentHighlightColors = defaultHighlightColors;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JComboBox<String> themeSelector;
    private JTable scoreTable;
    private DefaultTableModel scoreTableModel;

    public PianoMemoryGame() {
        playerId = JOptionPane.showInputDialog(this, "플레이어 이름을 입력하세요:", "플레이어 이름", JOptionPane.PLAIN_MESSAGE);
        if (playerId == null || playerId.trim().isEmpty()) {
            playerId = "플레이어";
        }

        setTitle("귀신이 된 피아노 레슨쌤");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단 레이블 생성
        createTopPanel();

        // 피아노 건반 생성
        createPianoKeys();

        // 테마 선택 메뉴 생성
        createThemeSelector();

        // 스코어보드 생성
        createScoreBoard();

        // 하단 패널 생성 (진행률 바와 버튼)
        createBottomPanel();

        loadHighScore();// 최고 점수 로드
        showGameInstructions(); //게임방법 첫 팝업
    }

    /**
     * 상단 패널 생성: 점수와 레벨 표시
     */
    private void createTopPanel() {
        JPanel scorePanel = new JPanel(new GridLayout(1, 3));
        levelLabel = new JLabel("레벨: " + level, SwingConstants.CENTER);
        scoreLabel = new JLabel("점수: " + score, SwingConstants.CENTER);
        highScoreLabel = new JLabel("최고 점수: " + highScore + " (" + highScorePlayer + ")", SwingConstants.CENTER);

        scorePanel.add(levelLabel);
        scorePanel.add(scoreLabel);
        scorePanel.add(highScoreLabel);

        add(scorePanel, BorderLayout.NORTH);
    }
    /**
     * 하단 패널 생성: 진행률 바와 컨트롤 버튼
     */
    private void createBottomPanel() {
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, INPUT_TIME_LIMIT);
        progressBar.setValue(INPUT_TIME_LIMIT);
        progressBar.setStringPainted(false);
        progressLabel = new JLabel("남은 시간: " + (INPUT_TIME_LIMIT / 1000) + "초", SwingConstants.CENTER);

        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(progressLabel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new GridLayout(1, 3));
        JButton startButton = new JButton("시작");
        startButton.addActionListener(e -> startGame());
        JButton helpButton = new JButton("게임 방법");
        helpButton.addActionListener(e -> showGameInstructions());
        JButton quitButton = new JButton("종료");
        quitButton.addActionListener(e -> System.exit(0));

        controlPanel.add(startButton);
        controlPanel.add(helpButton);
        controlPanel.add(quitButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressPanel, BorderLayout.NORTH);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 피아노 건반 생성: 8개 버튼
     */
    private void createPianoKeys() {
        JPanel buttonPanel = new JPanel(null);
        buttonPanel.setPreferredSize(new Dimension(800, 200));

        int whiteKeyWidth = 60;
        int whiteKeyHeight = 150;
        int xPosition = 0;

        for (int i = 0; i < pianoKeys.length; i++) {
            JButton whiteKey = new JButton(pianoKeys[i]);
            whiteKey.setBounds(xPosition, 0, whiteKeyWidth, whiteKeyHeight);
            whiteKey.setBackground(Color.WHITE);
            whiteKey.setOpaque(true);

            final int index = i; // 인덱스는 고정된 값을 참조해야 함
            whiteKey.addActionListener(e -> {
                // 현재 테마의 강조 색상 적용
                whiteKey.setBackground(currentHighlightColors[index]);

                // 300ms 후 원래 색상으로 복원
                Timer restoreTimer = new Timer(300, evt -> whiteKey.setBackground(Color.WHITE));
                restoreTimer.setRepeats(false);
                restoreTimer.start();

                handleUserInput(index);
            });

            buttons[i] = whiteKey;
            buttonPanel.add(whiteKey);
            xPosition += whiteKeyWidth;
        }

        add(buttonPanel, BorderLayout.CENTER);
    }
    /**
     * 테마 선택 드롭다운 생성
     */
    private void createThemeSelector() {
        String[] themes = {"기본 테마", "어두운 테마", "밝은 테마"};
        themeSelector = new JComboBox<>(themes);
        themeSelector.addActionListener(e -> {
            String selectedTheme = (String) themeSelector.getSelectedItem();
            switch (selectedTheme) {
                case "기본 테마":
                    currentHighlightColors = defaultHighlightColors;
                    break;
                case "어두운 테마":
                    currentHighlightColors = darkHighlightColors;
                    break;
                case "밝은 테마":
                    currentHighlightColors = brightHighlightColors;
                    break;
            }
        });
        add(themeSelector, BorderLayout.EAST);
    }
    /**
     * 점수표 생성: 플레이어와 점수 기록
     */
    private void createScoreBoard() {
        String[] columns = {"플레이어", "점수"};
        scoreTableModel = new DefaultTableModel(columns, 0);
        scoreTable = new JTable(scoreTableModel);
        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.setPreferredSize(new Dimension(200, 400));
        add(scrollPane, BorderLayout.WEST);
    }

    private void updateScoreBoard() {
        scoreTableModel.addRow(new Object[]{playerId, score}); // 현재 플레이어의 점수를 기록
    }

    private void startGame() {
        sequence.clear();
        userInput.clear();
        level = 1;
        score = 0;
        isGameEnded = false;
        updateLabels();
        addNextSequence();
        showSequence();
    }
    /**
     * 다음 시퀀스에 새로운 음계 추가
     */
    private void addNextSequence() {
        sequence.add(random.nextInt(8));
    }
    /**
     * 시퀀스를 화면에 표시
     */
    private void showSequence() {
        userInput.clear();
        Timer sequenceTimer = new Timer(1100, null);
        final int[] currentIndex = {0};

        sequenceTimer.addActionListener(e -> {
            if (currentIndex[0] < sequence.size()) {
                int index = sequence.get(currentIndex[0]);
                buttons[index].setBackground(currentHighlightColors[index]);
                playSound(buttonSounds[index]);//사운드 재상

                Timer restoreTimer = new Timer(500, evt -> buttons[index].setBackground(Color.WHITE));
                restoreTimer.setRepeats(false);
                restoreTimer.start();

                currentIndex[0]++;
            } else {
                ((Timer) e.getSource()).stop();
                startInputTimer();
            }
        });

        sequenceTimer.start(); //입력 타이머 시작
    }
    /**
     * 입력 제한 시간 타이머 시작
     */
    private void startInputTimer() {
        progressBar.setValue(INPUT_TIME_LIMIT);// 진행률 초기화
        progressLabel.setText("남은 시간: " + (INPUT_TIME_LIMIT / 1000) + "초");

        if (inputTimer != null) {
            inputTimer.stop();// 기존 타이머 종료
        }

        inputTimer = new Timer(100, new ActionListener() {
            int remainingTime = INPUT_TIME_LIMIT;

            @Override
            public void actionPerformed(ActionEvent e) {
                remainingTime -= 100;
                progressBar.setValue(remainingTime);
                progressLabel.setText("남은 시간: " + (remainingTime / 1000) + "초");

                if (remainingTime <= 0) {
                    ((Timer) e.getSource()).stop();
                    endGame("시간 초과! 게임 종료!");
                }
            }
        });
        inputTimer.start();
    }
    /**
     * 사용자 입력 처리
     */
    private void handleUserInput(int index) {
        if (isGameEnded) return;

        playSound(buttonSounds[index]);
        userInput.add(index);

        if (!isCurrentInputCorrect()) {
            endGame("틀렸습니다!!!\nGame over!");
            return;
        }

        if (userInput.size() == sequence.size()) {
            inputTimer.stop();
            score += level * 10;
            level++;
            updateLabels();
            addNextSequence();
            showSequence();
        }
    }

    /**
     * 현재 입력이 올바른지 확인
     */
    private boolean isCurrentInputCorrect() {
        for (int i = 0; i < userInput.size(); i++) {
            if (!userInput.get(i).equals(sequence.get(i))) {
                return false;
            }
        }
        return true;
    }
    /**
     * 게임 종료 처리
     */
    private void endGame(String message) {
        if (isGameEnded) return;

        isGameEnded = true;
        if (inputTimer != null) {
            inputTimer.stop();
        }

        boolean isNewHighScore = score > highScore;

        JOptionPane.showMessageDialog(this, message + " 최종 점수: " + score);

        if (isNewHighScore) {
            highScore = score;
            highScorePlayer = playerId; // 최고 점수 플레이어 갱신
            saveHighScore();
            JOptionPane.showMessageDialog(this, "축하합니다! 새로운 최고 점수: " + highScore, "최고 점수 달성!", JOptionPane.INFORMATION_MESSAGE);
        }

        updateScoreBoard();
        updateLabels();
    }
    /**
     * 사운드 재생
     */
    private void playSound(String soundFile) {
        try {
            File soundPath = new File(soundFile);
            if (soundPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            } else {
                System.err.println("사운드 파일을 찾을 수 없습니다: " + soundFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLabels() {
        levelLabel.setText("레벨: " + level);
        scoreLabel.setText("점수: " + score);
        highScoreLabel.setText("최고 점수: " + highScore + " (" + highScorePlayer + ")");
    }

    /**
     * 최고 점수 저장
     */
    private void saveHighScore() {
        File file = new File("highscore.txt");
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(highScorePlayer + "," + highScore); // 최고 점수 기록
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHighScore() {
        File file = new File("highscore.txt");
        if (!file.exists()) {
            highScore = 0;
            highScorePlayer = "플레이어";
            return;
        }

        try (Scanner scanner = new Scanner(file)) {
            if (scanner.hasNextLine()) {
                String[] data = scanner.nextLine().split(",");
                if (data.length == 2) {
                    highScorePlayer = data[0];
                    highScore = Integer.parseInt(data[1]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * 게임 방법 팝업 표시
     */
    private void showGameInstructions() {
        JOptionPane.showMessageDialog(this,
            "게임 방법:\n" +
            "0. 시작 버튼을 꼭 눌러주세요\n"
            + "1. 연주되는 피아노 건반의 순서를 기억하세요.\n" +
            "2. 순서에 따라 건반을 클릭하세요.\n" +
            "3. 레벨이 올라갈수록 순서가 한개씩 늘어납니다.\n" +
            "4. 다른 레슨자의 최고 점수를 깨보세요.\n"+
            "즐거운 게임 되세요!",
            "게임 방법",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PianoMemoryGame game = new PianoMemoryGame();
            game.setVisible(true);
        });
    }
}
