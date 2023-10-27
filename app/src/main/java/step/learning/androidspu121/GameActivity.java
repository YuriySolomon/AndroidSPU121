package step.learning.androidspu121;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.MediaParser;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private static final int N = 4; //розмір поля
    private int[][] cells = new int[N][N];  //дані комірок поля
    private TextView[][] tvCells = new TextView[N][N];  // View поля
    private int score;
    private TextView tvScore;
    private int bestScore;
    private TextView tvBestScore;
    private Button newGame;
    private Button back;
    private final Random random = new Random();
    private Animation spawnCellAnimation;
    private Animation collapseCellAnimation;
    private MediaPlayer spawnSound;
    private boolean win;

    @SuppressLint("DiscouragedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Заблокувати поворот пристрою
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        spawnSound = MediaPlayer.create(
                GameActivity.this,
                R.raw.pickup_00
        );


        spawnCellAnimation = AnimationUtils.loadAnimation(
                GameActivity.this,
                R.anim.game_spawn_cell
        );
        spawnCellAnimation.reset();

        collapseCellAnimation = AnimationUtils.loadAnimation(
                GameActivity.this,
                R.anim.game_collapse_cells
        );
        collapseCellAnimation.reset();

        tvScore = findViewById(R.id.game_tv_score);
        tvScore.setOnClickListener(v -> processMove(MoveDirection.LEFT));
        tvBestScore = findViewById(R.id.game_tv_best);
        tvBestScore.setOnClickListener(v -> processMove(MoveDirection.RIGHT));
        newGame = findViewById(R.id.game_btn_new);
        //newGame.setOnClickListener(v -> processMove(MoveDirection.TOP));
        newGame.setOnClickListener(v -> newGamesAct());
        back = findViewById(R.id.game_btn_undo);
        back.setOnClickListener(v -> processMove(MoveDirection.BOTTOM));

        // Отримання посилання на об'єкт SharedPreferences
        SharedPreferences preferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        // Отримання збереженого рядка за ключем "key" (за замовчуванням - "default_value", якщо ключ не знайдено)
        int savedValue = preferences.getInt("bestScore", 0);
        bestScore = savedValue;
        tvBestScore.setText(getString(R.string.game_tv_best, bestScore));

        // пошук ідентифікаторів за іменеь (String) та ресурсів через них
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                tvCells[i][j] = findViewById(
                    getResources() // R.
                            .getIdentifier(
                                    "game_cell_" + i + j,
                                    "id",                   //R.id
                                    getPackageName()
                ));
            }
        }

        // Задати ігровому полю висоту таку ж як ширину
        // Проблема на етапі OnCreate розміри ще не відомі
        TableLayout gameField = findViewById(R.id.game_field);
        gameField.post( // поставити задачу у чергу. вона буде виконана
                // коди gameField  виконає усі попередні задачі. у т.ч.
                // орозрахунок розміру та рисування.
                () -> {
                    int windowWidth = this.getWindow().getDecorView().getWidth();
                    int margin =
                            ((LinearLayout.LayoutParams)gameField.getLayoutParams()).leftMargin;
                    int fieldSize = windowWidth - 2 * margin;
                    LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(fieldSize, fieldSize);
                    layoutParams.setMargins(margin, margin, margin, margin);
                    gameField.setLayoutParams(layoutParams);
                }
        );

        findViewById(R.id.game_layout).setOnTouchListener(
                new OnSwipeListener(GameActivity.this){
                    @Override
                    public void onSwipeBottom() {
                        processMove(MoveDirection.BOTTOM);
                        /*Toast.makeText( // аповідомлення, що з'являється та зникає
                                GameActivity.this,  // контекст
                                "onSwipeBottom",    // повідомлення
                                Toast.LENGTH_SHORT  // тривалість (довжина у часі)
                        ).show();*/
                    }
                    @Override
                    public void onSwipeLeft() {
                        processMove(MoveDirection.LEFT);
                    }
                    @Override
                    public void onSwipeRight() {
                        processMove(MoveDirection.RIGHT);
                    }
                    @Override
                    public void onSwipeTop() {
                        processMove(MoveDirection.TOP);
                    }
        });

        newGames();
    }

    private void newGames() {
        win = false;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                cells[i][j] = 0;
            }
        }
        spawnCell();
        spawnCell();
        showField();
    }

    /**
     * Поставити нове число у випадкову вільну комірку
     * Значення: 2 (з імовірністью 0.9) або 4
     */
    private void spawnCell() {
        // шукаємо всі вільні комірки
        List<Integer> freeCellsIndexes = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (cells[i][j] == 0) {
                    freeCellsIndexes.add(i * N +j);
                }
            }
        }
        // визначаємо випадкову з вільних
        int randIndex = freeCellsIndexes.get(
                random.nextInt(freeCellsIndexes.size())
        );
        int x = randIndex / N;
        int y = randIndex % N;
        // заповнюємо комірку значенням
        cells[x][y] =
                random.nextInt(10) == 0   // умова з імов. 0.1
                ? 4
                : 2;
        // призначаємо анімацію для її представлення
        tvCells[x][y].startAnimation(spawnCellAnimation);
        // програємо звук
        spawnSound.start();
    }

    /**
     * Показ усіх комірок - відображення масиву чисел на ігрове полу
     */
    @SuppressLint("DiscouragedApi")
    private void showField() {
        // для кожної комірки визначаємо стиль у відповідності до
        // її значення та застосовуємо його.
        Resources resources = getResources();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                // сам текст комірки
                tvCells[i][j].setText(String.valueOf(cells[i][j]));
                // стиль !! але зі стилем застосовуються не всі атрибути
                tvCells[i][j].setTextAppearance(
                        resources.getIdentifier(
                                "game_cell_" + cells[i][j],
                                "style",    // R.style
                                getPackageName()
                        )
                );
                // фонові атрибути потрібно додати окремо (до стилів)
                tvCells[i][j].setBackgroundColor(
                        resources.getColor(
                                resources.getIdentifier(
                                        "game_cell_background_" + cells[i][j],
                                        "color",    //R.color
                                        getPackageName()
                                ),
                                getTheme()
                        )
                );
            }
        }

        // виводимо рахунок та кращий (заповнюючи плейсхолдер ресурсу
        tvScore.setText(getString(R.string.game_tv_score, score));

        SharedPreferences preferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (score > bestScore) {
            bestScore = score;
            editor.putInt("bestScore", bestScore);
            editor.apply();
        }
        tvBestScore.setText(getString(R.string.game_tv_best, bestScore));
    }

    private boolean canMoveBottom() {
        for (int j = 0; j < N; j++) {
            for (int i = 1; i < N; i++) {
                if (cells[i][j] == 0 && cells[i - 1][j] != 0 ||
                        cells[i][j] != 0 && cells[i][j] == cells[i - 1][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveBottom() {
        for (int j = 0; j < N; j++) {
            // 1. вверх
            boolean needRepeat;
            do {
                needRepeat = false;
                for (int i = N - 1; i > 0; i--) {
                    if (cells[i][j] == 0 && cells[i - 1][j] != 0) {
                        cells[i][j] = cells[i - 1][j];
                        cells[i - 1][j] = 0;
                        needRepeat = true;
                    }
                }
            } while(needRepeat);

            // 2. коллапс
            for (int i = N - 1; i > 0; i--) {
                if (cells[i][j] != 0 && cells[i][j] == cells[i - 1][j]) {
                    cells[i][j] += cells[i - 1][j];
                    score += cells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);
                    // 3. Переміщуємо на "злите" місце
                    for (int k = i - 1; k > 0; k--) {
                        cells[k][j] = cells[k - 1][j];
                    }
                    // занулюємо першу
                    cells[0][j] = 0;
                }
            }

        }
    }
    private boolean canMoveLeft() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                if (cells[i][j] == 0 && cells[i][j + 1] != 0 ||
                    cells[i][j] != 0 && cells[i][j] == cells[i][j + 1]) {
                    return true;
                }
            }
        }
        return false;
    }
    private void moveLeft() {                                   // [2028]
        // 1. Переміщуємо всі значення ліворуч (без "пробілів")    [2280]
        // 2. Перевіряємо коллапси (злиття), зливаємо              [4-80]
        // 3. Повторюємо п. 1 після злиття                         [4800]
        // 4/ Повторюємо пп.1-3 для кожного рядка
        for (int i = 0; i < N; i++) {
            // 1. ліворуч
            boolean needRepeat;
            do {
                needRepeat = false;
                for (int j = 0; j < N - 1; j++) {
                    if (cells[i][j] == 0 && cells[i][j + 1] != 0) {
                        cells[i][j] = cells[i][j + 1];
                        cells[i][j + 1] = 0;
                        needRepeat = true;
                    }
                }
            } while(needRepeat);

            // 2. коллапс
            for (int j = 0; j < N - 1; j++) {
                if (cells[i][j] != 0 && cells[i][j] == cells[i][j + 1]) { // [2284]
                    cells[i][j] += cells[i][j + 1];                       // [4284]
                    score += cells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);
                    // 3. Переміщуємо на "злите" місце
                    for (int k = j + 1; k < N - 1; k++) {                  // [4844]
                        cells[i][k] = cells[i][k + 1];
                    }
                    // занулюємо останню                                       [4840]
                    cells[i][N -1] = 0;
                }
            }

        }
    }

    private boolean canMoveRight() {
        for (int i = 0; i < N; i++) {
            for (int j = 1; j < N; j++) {
                if (cells[i][j] == 0 && cells[i][j - 1] != 0 ||
                        cells[i][j] != 0 && cells[i][j] == cells[i][j - 1]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveRight() {
        for (int i = 0; i < N; i++) {
            // 1. праворуч
            boolean needRepeat;
            do {
                needRepeat = false;
                for (int j = N - 1; j > 0; j--) {
                    if (cells[i][j] == 0 && cells[i][j - 1] != 0) {
                        cells[i][j] = cells[i][j - 1];
                        cells[i][j - 1] = 0;
                        needRepeat = true;
                    }
                }
            } while(needRepeat);

            // 2. коллапс
            for (int j = N - 1; j > 0; j--) {
                if (cells[i][j] != 0 && cells[i][j] == cells[i][j - 1]) { // [4222]
                    cells[i][j] += cells[i][j - 1];                       // [4224]
                    score += cells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);
                    // 3. Переміщуємо на "злите" місце
                    for (int k = j - 1; k > 0; k--) {                  // []
                        cells[i][k] = cells[i][k - 1];
                    }
                    // занулюємо першу                                       [0424]
                    cells[i][0] = 0;
                }
            }

        }
    }

    private boolean canMoveTop() {
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < N - 1; i++) {
                if (cells[i][j] == 0 && cells[i + 1][j] != 0 ||
                        cells[i][j] != 0 && cells[i][j] == cells[i + 1][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveTop() {
        for (int j = 0; j < N; j++) {
            // 1. вверх
            boolean needRepeat;
            do {
                needRepeat = false;
                for (int i = 0; i < N - 1; i++) {
                    if (cells[i][j] == 0 && cells[i +1][j] != 0) {
                        cells[i][j] = cells[i + 1][j];
                        cells[i + 1][j] = 0;
                        needRepeat = true;
                    }
                }
            } while(needRepeat);

            // 2. коллапс
            for (int i = 0; i < N - 1; i++) {
                if (cells[i][j] != 0 && cells[i][j] == cells[i + 1][j]) {
                    cells[i][j] += cells[i + 1][j];
                    score += cells[i][j];
                    tvCells[i][j].startAnimation(collapseCellAnimation);
                    // 3. Переміщуємо на "злите" місце
                    for (int k = i + 1; k < N - 1; k++) {
                        cells[k][j] = cells[k + 1][j];
                    }
                    // занулюємо останню
                    cells[N - 1][j] = 0;
                }
            }

        }
    }

    private void newGamesAct() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        SpannableString message = new SpannableString("Ви хочете розпочати нову гру?");
        message.setSpan(new RelativeSizeSpan(1.5f), 0, message.length(), 0); // Збільшити текст на 1.5 рази
        builder.setTitle("Game Info")
                .setMessage(message)
                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        recreate();
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void lostGames() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // Заборонити закриття діалогу кнопкою "Назад"

        SpannableString message = new SpannableString("Ви програли.");
        message.setSpan(new RelativeSizeSpan(2.0f), 0, message.length(), 0); // Збільшити текст на 1.5 рази
        builder.setTitle("Game Info")
                .setMessage(message)
                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Обробка натискання кнопки "ОК" в діалозі
                        dialog.dismiss(); // Закриття діалогу після взаємодії користувача
                    }
                });

        AlertDialog dialog = builder.create();
        // для тестів
        //if (canMove(MoveDirection.BOTTOM) && canMove(MoveDirection.LEFT) && canMove(MoveDirection.RIGHT) && canMove(MoveDirection.TOP)) {
        if (! canMove(MoveDirection.BOTTOM) && ! canMove(MoveDirection.LEFT) && ! canMove(MoveDirection.RIGHT) && ! canMove(MoveDirection.TOP)) {
            //Toast.makeText(GameActivity.this, "Ви програли", Toast.LENGTH_LONG).show();
            dialog.show();
        }
    }
    private boolean winGames() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (cells[i][j] == 2048) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean canMove(MoveDirection direction){
        switch (direction) {
            case BOTTOM: return canMoveBottom();
            case LEFT: return canMoveLeft();
            case RIGHT: return canMoveRight();
            case TOP: return canMoveTop();
        }
        return false;
    }
    private void move(MoveDirection direction) {
        switch (direction) {
            case BOTTOM: moveBottom(); break;
            case LEFT: moveLeft(); break;
            case RIGHT: moveRight(); break;
            case TOP: moveTop(); break;
        }
    }
    private void processMove(MoveDirection direction) {
        if (canMove(direction)) {
            move(direction);
            spawnCell();
            showField();
        }
        else {
            Toast.makeText(GameActivity.this, "Ходу немає", Toast.LENGTH_SHORT).show();
        }
        lostGames();
        if ( ! win && winGames()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false); // Заборонити закриття діалогу кнопкою "Назад"

            SpannableString message = new SpannableString("Ви виграли.");
            message.setSpan(new RelativeSizeSpan(2.0f), 0, message.length(), 0); // Збільшити текст на 1.5 рази
            builder.setTitle("Game Info")
                    .setMessage(message)
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
            win = true;
            }
        }

    private enum MoveDirection {
        BOTTOM,
        LEFT,
        RIGHT,
        TOP
    }
}
/*
Д.З. Підібрати кольори для овормлення всіх станів гри (2048)
Реалізувати їх ресурси за єдиним правилом "префікс_{значення}"
Випробувати через впровадження стилів
 */
/*
Д.З. Завершити проєкт 2048
 - Додати рухи вгору та вниз
 - Реалізувати кінець гри:
  = неможливість рухів - програш
  = набір 2048 хоча б в одній комірці - виграш
   - тут можна реалізувати вибір продовження гри
     до програшу без повторних повідомлень про виграш
 - Рекорд та його збереження (на постійній основі - у файл)
 * Кнопка "назад" - повернення попереднього стану (рух назад)
  - відновлення стану поля
  - повернення рахунку
  - перевірка рекорду
 - Заблокувати поворот пристрою або
 * зробити ландшафтну орієнтацію.
 - Додати звуків та засобів управління ними (гучність або вимикач)
 */