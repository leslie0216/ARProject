package edu.dhbw.andar.sample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * Created by chl848 on 02/04/2015.
 */
public class DrawView extends View {

    Paint m_paint;

    private String m_id;
    private String m_name;

    private int m_color;

    private static final int m_textSize = 50;
    private static final int m_messageTextSize = 50;
    private static final int m_textStrokeWidth = 2;
    private static final int m_boundaryStrokeWidth = 10;

    private String m_message;

    private float m_x, m_y, m_z;

    private ArrayList<Ball> m_balls;
    private int m_touchedBallId;

    private class Ball {
        public int m_ballColor;
        public float m_ballX;
        public float m_ballY;
        public boolean m_isTouched;
        public String m_id;
        public String m_name;
    }

    private float m_ballRadius;
    private float m_ballBornX;
    private float m_ballBornY;

    private float m_localCoordinateCenterX;
    private float m_localCoordinateCenterY;
    private float m_localCoordinateRadius;

    private float [] m_glMatrix;

    private enum Quadrant {
        none,
        one,
        two,
        three,
        four
    }

    private float m_angle;

    public class RemotePhoneInfo {
        String m_name;
        int m_color;
        float m_angle;
    }

    private ArrayList<RemotePhoneInfo> m_remotePhones;
    private float m_remotePhoneRadius;

    private boolean m_showRemoteNames;
    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        @Override
        public void run() {
            setShowRemoteNames(true);
            m_numberOfLongPress++;
            invalidate();
        }
    };

    /**
     * experiment begin
     */
    private ArrayList<String> m_ballNames;
    private long m_trailStartTime;
    private int m_numberOfDrops;
    private int m_numberOfErrors;
    private int m_numberOfTouch;
    private int m_numberOfTouchBall;
    private int m_numberOfLongPress;
    private int m_numberOfRelease;
    private String m_receiverName;
    private int m_maxBlocks;
    private int m_maxTrails;
    private int m_currentBlock;
    private int m_currentTrail;
    private static final int m_experimentPhoneNumber = 3;
    private MainLogger m_logger;
    private MainLogger m_angleLogger;
    private MainLogger m_matrixLogger;
    private boolean m_isStarted;
    /**
     * experiment end
     */

    public DrawView(Context context) {
        super(context);
        m_paint = new Paint();
        m_x = m_y = m_z = 0.0f;

        m_angle = 0.0f;

        m_touchedBallId = -1;
        m_balls = new ArrayList<>();

        m_remotePhones = new ArrayList<>();

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        initBallBornPoints(displayMetrics);

        m_message = "No Message";

        m_glMatrix = new float[16];

        m_id = ((CustomActivity)(context)).getUserId();
        m_name = ((CustomActivity)(context)).getUserName();
        Random rnd = new Random();
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        m_localCoordinateCenterX = displayMetrics.widthPixels * 0.5f;
        m_localCoordinateCenterY = displayMetrics.heightPixels * 0.9f;
        m_localCoordinateRadius = displayMetrics.widthPixels * 0.5f;

        m_remotePhoneRadius = displayMetrics.heightPixels * 0.05f;

        setShowRemoteNames(false);

        resetCounters();

        m_isStarted = false;
    }

    private void initBallBornPoints(DisplayMetrics displayMetrics) {
        m_ballRadius = displayMetrics.heightPixels * 0.08f;
        m_ballBornX = displayMetrics.widthPixels * 0.5f;
        m_ballBornY = displayMetrics.heightPixels * 0.9f - m_ballRadius * 2.0f;
    }

    private void setShowRemoteNames(boolean show) {
        m_showRemoteNames = show;
    }

    private boolean getShowRemoteNames() {
        return m_showRemoteNames;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // debug ++

        //showGlMatrix(canvas);

        //showGlTranslation(canvas);

        //showCircleCoordinate(canvas);

        // debug --
        //showOffset(canvas);
        showLocalCircleCoordinate(canvas);
        showMessage(canvas);
        showBalls(canvas);
        showBoundary(canvas);
        showLocalAngle(canvas);
        showProgress(canvas);
    }

    public void setMessage (String msg) {
        m_message = msg;
    }

    public void updateXYZ(float x, float y, float z)
    {
        m_x = x;
        m_y = y;
        m_z = z;
    }

    public void updateglMatrix(float[] matrix) {
        for (int i=0; i<16; i++){
            m_glMatrix[i] = matrix[i];
        }

        calculateAngle();

        long timestamp = System.currentTimeMillis();

        if (m_isStarted) {
            // log angle
            if (m_angleLogger != null) {
                //<participantID> <participantName> <condition> <block#> <trial#> <angle> <timestamp>
                m_angleLogger.write(m_id + "," + m_name + "," + getResources().getString(R.string.app_name) + "," + m_currentBlock + "," + m_currentTrail + "," + m_angle + "," + timestamp, false);
            }

            // log matrix
            //<participantID> <participantName> <condition> <block#> <trial#> <M00> <M01> <M02> <M03> <M10> <M11> <M12> <M13> <M20> <M21> <M22> <M23> <M30> <M31> <M32> <M33> <timestamp>
            if (m_matrixLogger != null) {
                m_matrixLogger.write(m_id + "," + m_name + "," + getResources().getString(R.string.app_name) + "," + m_currentBlock + "," + m_currentTrail + "," + m_glMatrix[0] + "," + m_glMatrix[4] + "," + m_glMatrix[8] + "," + m_glMatrix[12] + "," + m_glMatrix[1] + "," + m_glMatrix[5] + "," + m_glMatrix[9] + "," + m_glMatrix[13] + "," + m_glMatrix[2] + "," + m_glMatrix[6] + "," + m_glMatrix[10] + "," + m_glMatrix[14] + "," + m_glMatrix[3] + "," + m_glMatrix[7] + "," + m_glMatrix[11] + "," + m_glMatrix[15] + "," + timestamp, false);
            }
        }
    }

    public void showGlMatrix(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.RED);
        String output = String.format("%.4f", m_glMatrix[0]) + " " + String.format("%.4f", m_glMatrix[4]) + " " + String.format("%.4f", m_glMatrix[8]) + " " + String.format("%.4f", m_glMatrix[12]);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(output, 50.0f, displayMetrics.heightPixels - 145.0f, m_paint);

        String output2 = String.format("%.4f", m_glMatrix[1]) + " " + String.format("%.4f", m_glMatrix[5]) + " " + String.format("%.4f", m_glMatrix[9]) + " " + String.format("%.4f", m_glMatrix[13]);
        canvas.drawText(output2, 50.0f, displayMetrics.heightPixels - 85.0f, m_paint);

        String output3 = String.format("%.4f", m_glMatrix[2]) + " " + String.format("%.4f", m_glMatrix[6]) + " " + String.format("%.4f", m_glMatrix[10]) + " " + String.format("%.4f", m_glMatrix[14]);
        canvas.drawText(output3, 50.0f, displayMetrics.heightPixels - 25.0f, m_paint);
    }

    public void showGlTranslation(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.RED);
        float dist = (float)Math.sqrt(m_glMatrix[12] * m_glMatrix[12] + m_glMatrix[13] * m_glMatrix[13] + m_glMatrix[14] * m_glMatrix[14]);
        String output = "X=" + String.format("%.4f", m_glMatrix[12]) + " Y=" + String.format("%.4f", m_glMatrix[13]) + " Z=" + String.format("%.4f", m_glMatrix[14]) + " dist=" + String.format("%.4f", dist);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(output, 50.0f, displayMetrics.heightPixels - 85.0f, m_paint);
    }

    public void showCircleCoordinate(Canvas canvas){
        Quadrant quadrant = calculateAngle();

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float centerX = displayMetrics.widthPixels * 0.5f;
        float centerY = displayMetrics.heightPixels * 0.5f;
        float radius = 300.0f;

        float pointX = centerX + radius * (float)Math.cos(Math.toRadians(m_angle));
        float pointY = centerY - radius * (float) Math.sin(Math.toRadians(m_angle));

        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(centerX, centerY, radius, m_paint);

        if (quadrant != Quadrant.none) {
            m_paint.setColor(Color.RED);
            m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawCircle(pointX, pointY, 10, m_paint);
        }


        m_paint.setTextSize(30);
        m_paint.setColor(Color.YELLOW);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        canvas.drawText("value1 : " + String.format("%.4f", m_glMatrix[0]), 50.0f, 150.0f, m_paint);
        canvas.drawText("value2 : " + String.format("%.4f", m_glMatrix[4]), 50.0f, 200.0f, m_paint);
        canvas.drawText("angle : " + String.format("%.4f", m_angle), 50.0f, 250.0f, m_paint);
        canvas.drawText("quadrant : " + quadrant.toString(), 50.0f, 300.0f, m_paint);

    }

    public void showLocalCircleCoordinate(Canvas canvas){
       DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float crosshairX = displayMetrics.widthPixels * 0.5f;
        float crosshairY = displayMetrics.heightPixels * 0.5f;
        float crosshairRadius = 50.0f;

        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.STROKE);

        // draw crosshair
        canvas.drawCircle(crosshairX, crosshairY, crosshairRadius, m_paint);
        canvas.drawLine(crosshairX - crosshairRadius * 2, crosshairY, crosshairX + crosshairRadius * 2, crosshairY, m_paint);
        canvas.drawLine(crosshairX, crosshairY - crosshairRadius * 2, crosshairX, crosshairY + crosshairRadius * 2, m_paint);

        // draw coordinate
        float left = 0.0f;
        float top = displayMetrics.heightPixels * 0.9f - m_localCoordinateRadius;
        float right = displayMetrics.widthPixels;
        float bottom = displayMetrics.heightPixels * 0.9f + m_localCoordinateRadius;
        RectF disRect = new RectF(left, top, right, bottom);

        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        canvas.drawArc(disRect, 180.0f, 180.0f, false, m_paint);

        CustomActivity customActivity = (CustomActivity)getContext();
        if ((customActivity != null) && customActivity.isConnected()) {
            showRemotePhones(canvas);
        }
    }

    public void showLocalAngle(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        m_paint.setStrokeWidth(m_textStrokeWidth);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText("angle : " + String.format("%.4f", m_angle), displayMetrics.widthPixels * 0.1f, displayMetrics.heightPixels * 0.95f, m_paint);
    }

    public void showRemotePhones(Canvas canvas) {
        if (!m_remotePhones.isEmpty()) {
            int size = m_remotePhones.size();
            for (int i=0; i<size; ++i) {
                RemotePhoneInfo info = m_remotePhones.get(i);
                float angle_remote = calculateRemoteAngle(info.m_angle);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));
                m_paint.setColor(info.m_color);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(pointX, pointY, m_remotePhoneRadius, m_paint);

                if (getShowRemoteNames()) {
                    m_paint.setTextSize(m_textSize);
                    m_paint.setStrokeWidth(m_textStrokeWidth);
                    float textX = pointX - m_remotePhoneRadius;
                    float textY = pointY - m_remotePhoneRadius * 1.5f;
                    if (info.m_name.length() > 5) {
                        textX = pointX - m_remotePhoneRadius * 2.0f;
                    }
                    canvas.drawText(info.m_name, textX, textY, m_paint);
                }
            }
        }
    }

    public void showOffset(Canvas canvas) {
        m_paint.setTextSize(50);
        m_paint.setColor(Color.GREEN);
        float dist = (float)Math.sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
        String output = "X=" + String.format("%.4f", m_x) + " Y=" + String.format("%.4f", m_y) + " Z=" + String.format("%.4f", m_z) + " dist=" + String.format("%.4f", dist);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        //canvas.drawText(output, 50.0f, displayMetrics.heightPixels - 25.0f, m_paint);
        canvas.drawText(output, 200.0f, (int)(displayMetrics.heightPixels * 0.1), m_paint);
    }

    public void showMessage(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.GREEN);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(m_message, displayMetrics.widthPixels * 0.4f, displayMetrics.heightPixels * 0.95f, m_paint);
    }

    public void showBalls(Canvas canvas) {
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        for (Ball ball : m_balls) {
            m_paint.setColor(ball.m_ballColor);
            canvas.drawCircle(ball.m_ballX, ball.m_ballY, m_ballRadius, m_paint);

            /**
             * experiment begin
             */
            m_paint.setStrokeWidth(m_textStrokeWidth);
            float textX = ball.m_ballX - m_ballRadius;
            float textY = ball.m_ballY - m_ballRadius;
            if (ball.m_name.length() > 5) {
                textX = ball.m_ballX - m_ballRadius * 2.0f;
            }
            canvas.drawText(ball.m_name, textX, textY, m_paint);
            /**
             * experiment end
             */
        }
    }

    public void showBoundary(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawLine(0, displayMetrics.heightPixels * 0.9f, displayMetrics.widthPixels, displayMetrics.heightPixels * 0.9f, m_paint);
    }

    public void showProgress(Canvas canvas) {
        m_paint.setTextSize(m_textSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String block = "Block: " + m_currentBlock +"/" + m_maxBlocks;
        canvas.drawText(block, (int) (displayMetrics.widthPixels * 0.65), (int) (displayMetrics.heightPixels * 0.95), m_paint);

        String trial = "Trial: " + m_currentTrail +"/" + m_maxTrails;
        canvas.drawText(trial, (int) (displayMetrics.widthPixels * 0.8), (int) (displayMetrics.heightPixels * 0.95), m_paint);
    }

    private Quadrant calculateAngle() {
        float value1 = m_glMatrix[0];
        float value2 = m_glMatrix[4];

        Quadrant quadrant = Quadrant.none;

        if (value1 > 0.0f) {
            if (value2 > 0.0f) {
                quadrant = Quadrant.four;
            }
            else {
                quadrant = Quadrant.three;
            }
        }
        else {
            if (value2 > 0.0f) {
                quadrant = Quadrant.one;
            }
            else {
                quadrant = Quadrant.two;
            }
        }

        if (value1 == 0 && value2 == 0) {
            quadrant = Quadrant.none;
        }

        m_angle = 0.0f;
        value1 = Math.abs(value1);
        switch (quadrant) {
            case one:
                m_angle = 90.0f * value1;
                break;
            case two:
                m_angle = 90.0f * (1 - value1) + 90.0f;
                break;
            case three:
                m_angle = 90.0f * value1 + 90.0f * 2.0f;
                break;
            case four:
                m_angle = 90.0f * (1 - value1) + 90.0f * 3.0f;
                break;
        }

        return quadrant;
    }

    private float calculateRemoteAngle(float raw_angle) {
        float included_angle = Math.abs(m_angle - raw_angle);
        boolean greaterThan180 = false;

        if (included_angle > 180.0f) {
            included_angle = 360.0f - included_angle;
            greaterThan180 = true;
        }

        float new_remote_angle;

        float intersect_angle = (180.0f - included_angle) / 2.0f;

        if (m_angle > raw_angle) {
            if (!greaterThan180) {
                new_remote_angle = 90.0f + intersect_angle;
            } else {
                new_remote_angle = 90.0f - intersect_angle;
            }
        } else {
            if (!greaterThan180) {
                new_remote_angle = 90.0f - intersect_angle;
            } else {
                new_remote_angle = 90.0f + intersect_angle;
            }
        }

        return new_remote_angle;
    }

    @Override
     public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();

        switch (eventaction) {
            case MotionEvent.ACTION_DOWN:
                m_numberOfTouch++;
                m_touchedBallId = -1;
                for (int i = 0; i < m_balls.size(); ++i){
                    Ball ball = m_balls.get(i);
                    ball.m_isTouched = false;

                    double dist;
                    dist = Math.sqrt(Math.pow((X - ball.m_ballX), 2) + Math.pow((Y - ball.m_ballY), 2));
                    if (dist <= (touchRadius + m_ballRadius)) {
                        ball.m_isTouched = true;
                        m_touchedBallId = i;

                        boolean isOverlap = false;
                        for (int j = 0; j < m_balls.size(); ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist2 = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist2 <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap && !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }

                    if (m_touchedBallId > -1)
                    {
                        break;
                    }
                }

                if (m_touchedBallId == -1) {

                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        float angle_remote = calculateRemoteAngle(remotePhone.m_angle);
                        float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                        float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));

                        double dist = Math.sqrt(Math.pow((X - pointX),2) + Math.pow((Y - pointY), 2));

                        if (dist <= (touchRadius + m_remotePhoneRadius)) {
                            show = true;
                            break;
                        }
                    }

                    if (show) {
                        handler.postDelayed(mLongPressed, 1000);
                    }
                } else {
                    m_numberOfTouchBall++;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (getShowRemoteNames()) {
                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        float angle_remote = calculateRemoteAngle(remotePhone.m_angle);
                        float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                        float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));

                        double dist = Math.sqrt(Math.pow((X - pointX),2) + Math.pow((Y - pointY), 2));

                        if (dist <= (touchRadius + m_remotePhoneRadius)) {
                            show = true;
                            break;
                        }
                    }

                    if (!show) {
                        handler.removeCallbacks(mLongPressed);
                        setShowRemoteNames(false);
                        invalidate();
                    }
                }

                if (m_touchedBallId > -1) {
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < m_balls.size(); ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap & !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(mLongPressed);
                if (getShowRemoteNames()) {
                    setShowRemoteNames(false);
                    invalidate();
                }

                m_numberOfRelease++;

                if (m_touchedBallId > -1) {
                    m_numberOfDrops += 1;
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < m_balls.size(); ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap) {
                            String name = isSending(ball.m_ballX, ball.m_ballY);
                            if (!ball.m_name.isEmpty() && !name.isEmpty()) {
                                if (name.equalsIgnoreCase(ball.m_name)) {
                                    //((CustomActivity) getContext()).showToast("send ball to : " + name);
                                    //sendBall(ball, id);
                                    removeBall(ball.m_id);
                                    this.invalidate();
                                    endTrail();
                                } else {
                                    m_numberOfErrors += 1;
                                }
                            }
                        }
                    }
                }

                for (Ball ball : m_balls) {
                    ball.m_isTouched = false;
                }
                break;
        }

        return  true;
    }

    private boolean isBoundary(float x, float y) {
        boolean rt = false;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        while (true) {
            // check bottom
            if ((y + m_ballRadius) >= (displayMetrics.heightPixels * 0.9f)) {
                rt = true;
                break;
            }

            // check left
            if (x - m_ballRadius <= 0.0f) {
                rt = true;
                break;
            }

            // check right
            if (x + m_ballRadius >= displayMetrics.widthPixels) {
                rt = true;
                break;
            }

            //check top
            double dist = Math.sqrt(Math.pow((x - m_localCoordinateCenterX), 2) + Math.pow((y - m_localCoordinateCenterY), 2));
            if (dist + m_ballRadius >= m_localCoordinateRadius) {
                rt = true;
            }
            break;
        }

        return rt;
    }

    private boolean canTouch(float x, float y) {
        boolean rt = true;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        while (true) {
            // check bottom
            if (y >= (displayMetrics.heightPixels * 0.9f)) {
                rt = false;
                break;
            }

            // check left
            if (x <= 0.0f) {
                rt = false;
                break;
            }

            // check right
            if (x >= displayMetrics.widthPixels) {
                rt = false;
                break;
            }

            //check top
            double dist = Math.sqrt(Math.pow((x - m_localCoordinateCenterX), 2) + Math.pow((y - m_localCoordinateCenterY), 2));
            if (dist + m_ballRadius >= m_localCoordinateRadius) {
                rt = false;
            }
            break;
        }

        return rt;
    }

    private String isSending(float x, float y) {
        String receiverName = "";
        float rate = 10000.0f;
        if (!m_remotePhones.isEmpty()) {
            for (RemotePhoneInfo remotePhoneInfo : m_remotePhones) {
                float angle_remote = calculateRemoteAngle(remotePhoneInfo.m_angle);
                float pointX = m_localCoordinateCenterX + m_localCoordinateRadius * (float)Math.cos(Math.toRadians(angle_remote));
                float pointY = m_localCoordinateCenterY - m_localCoordinateRadius * (float)Math.sin(Math.toRadians(angle_remote));

                double dist = Math.sqrt(Math.pow((x - pointX), 2) + Math.pow((y - pointY), 2));
                if (dist < (m_remotePhoneRadius + m_ballRadius)){
                    if (dist < rate) {
                        receiverName = remotePhoneInfo.m_name;
                        rate = (float)dist;
                    }
                }
            }
        }

        return receiverName;
    }

    public void addBall() {
        Ball ball = new Ball();
        Random rnd = new Random();
        ball.m_ballColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        ball.m_ballX = m_ballBornX;
        ball.m_ballY = m_ballBornY;
        ball.m_isTouched = false;
        ball.m_id = UUID.randomUUID().toString();
        ball.m_name = getBallName();
        m_receiverName = ball.m_name;
        m_balls.add(ball);
    }

    public  void removeBall(String id) {
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                m_balls.remove(ball);
                m_touchedBallId = -1;
                break;
            }
        }
    }

    public void receivedBall(String id, int color) {
        boolean isReceived = false;
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                isReceived = true;
                break;
            }
        }

        if (!isReceived) {
            Ball ball = new Ball();
            ball.m_id = id;
            ball.m_ballColor = color;
            ball.m_isTouched = false;

            ball.m_ballX = m_ballBornX;
            ball.m_ballY = m_ballBornY;

            m_balls.add(ball);
        }
    }

    public void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void sendBall(Ball ball, String receiverName) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ballId", ball.m_id);
            jsonObject.put("ballColor", ball.m_ballColor);
            jsonObject.put("receiverName", receiverName);
            jsonObject.put("x", 0.0f);
            jsonObject.put("y", 0.0f);
            jsonObject.put("z", m_angle);
            jsonObject.put("isSendingBall", true);
            jsonObject.put("color", m_color);
            jsonObject.put("name", m_name);
        } catch (JSONException e){
            e.printStackTrace();
        }

        CustomActivity ca = (CustomActivity)getContext();
        if (ca != null) {
            ca.addMessage(jsonObject.toString());
        }
    }

    public void sendLocation(){
        JSONObject msg = new JSONObject();
        try {
            msg.put("x", 0.0f);
            msg.put("y", 0.0f);
            msg.put("z", m_angle);
            msg.put("name", m_name);
            msg.put("color", m_color);
            msg.put("isSendingBall", false);
        } catch (JSONException e){
            e.printStackTrace();
        }

        CustomActivity ca = (CustomActivity)getContext();
        if (ca != null) {
            ca.addMessage(msg.toString());
        }
    }

    public void updateRemotePhone(String name, int color, float angle){
        if (name.isEmpty() || name.equalsIgnoreCase(m_name)) {
            return;
        }

        int size = m_remotePhones.size();
        boolean isFound = false;
        for (int i = 0; i<size; ++i) {
            RemotePhoneInfo info = m_remotePhones.get(i);
            if (info.m_name.equalsIgnoreCase(name)) {
                info.m_color = color;
                info.m_angle = angle;
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            RemotePhoneInfo info = new RemotePhoneInfo();
            info.m_name = name;
            info.m_color = color;
            info.m_angle = angle;
            m_remotePhones.add(info);

            /**
             * experiment end
             */
            if (m_remotePhones.size() == m_experimentPhoneNumber) {
                initExperiment();
            }
            /**
             * experiment end
             */
        }
    }

    public ArrayList<RemotePhoneInfo> getRemotePhones() {
        return m_remotePhones;
    }

    public void removePhones(ArrayList<RemotePhoneInfo> phoneInfos) {
        m_remotePhones.removeAll(phoneInfos);
    }

    public void clearRemotePhoneInfo() {
        m_remotePhones.clear();
    }

    public int getBallCount() {
        return m_balls.size();
    }

    /**
     * experiment begin
     */
    private void initExperiment() {
        // init ball names
        m_ballNames = new ArrayList<>();

        m_maxBlocks = 5;
        m_maxTrails = 9;

        m_currentBlock = 0;
        m_currentTrail = 0;

        m_isStarted = false;

        resetBlock();

        m_logger = new MainLogger(getContext(), m_id + "_" + m_name + "_" + getResources().getString(R.string.app_name));
        //<participantID> <participantName> <condition> <block#> <trial#> <receiver name> <elapsed time for this trial> <number of errors for this trial> <number of release for this trial> <number of drops for this trial> <number of touch for this trial> <number of touch ball for this trial> <number of long press for this trial> <timestamp>
        m_logger.writeHeaders("participantID" + "," + "participantName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "receiverName" + "," + "elapsedTime" + "," + "errors" + "," + "release" + "," + "drops" + "," + "touch" + "," + "touchBall" + "," + "longPress" + "," + "timestamp");

        m_angleLogger = new MainLogger(getContext(), m_id+"_"+m_name+"_"+getResources().getString(R.string.app_name)+"_angle");
        //<participantID> <participantName> <condition> <block#> <trial#> <angle> <timestamp>
        m_angleLogger.writeHeaders("participantID" + "," + "participantName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "angle" + "," + "timestamp");

        m_matrixLogger = new MainLogger(getContext(), m_id+"_"+m_name+"_"+getResources().getString(R.string.app_name)+"_matrix");
        //<participantID> <participantName> <condition> <block#> <trial#> <M00> <M01> <M02> <M03> <M10> <M11> <M12> <M13> <M20> <M21> <M22> <M23> <M30> <M31> <M32> <M33> <timestamp>
        m_matrixLogger.writeHeaders("participantID" + "," + "participantName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "M00" + "," + "M01" + "," + "M02" + "," + "M03" + "," + "M10" + "," + "M11" + "," + "M12" + "," + "M13" + "," + "M20" + "," + "M21" + "," + "M22" + "," + "M23" + "," + "M30" + "," + "M31" + "," + "M32" + "," + "M33" + "," + "timestamp");

        ((CustomActivity)getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((CustomActivity) getContext()).setStartButtonEnabled(true);
                ((CustomActivity) getContext()).setContinueButtonEnabled(false);
            }
        });
    }

    private String getBallName() {
        if (m_ballNames.isEmpty()) {
            return "";
        }

        Random rnd = new Random();
        int index = rnd.nextInt(m_ballNames.size());
        String name = m_ballNames.get(index);
        m_ballNames.remove(index);
        return name;
    }

    public boolean isFinished() {
        return m_currentBlock == m_maxBlocks;
    }

    public void nextBlock() {
        ((CustomActivity)getContext()).setStartButtonEnabled(true);
        ((CustomActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void resetBlock() {
        // reset ball names
        m_ballNames.clear();
        for (RemotePhoneInfo remotePhoneInfo : m_remotePhones){
            for(int i=0; i<3; i++){
                m_ballNames.add(remotePhoneInfo.m_name);
            }
        }

        // reset self color
        Random rnd = new Random();
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        resetCounters();
    }

    public void startBlock() {
        m_currentBlock += 1;
        m_currentTrail = 0;
        m_isStarted = true;

        resetBlock();
        startTrial();
        ((CustomActivity)getContext()).setStartButtonEnabled(false);
        ((CustomActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void endBlock() {
        m_isStarted = false;

        if (isFinished()) {
            closeLogger();
        }

        new AlertDialog.Builder(getContext()).setTitle("Warning").setMessage("You have completed block " + m_currentBlock + ", please wait for other participants.").setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();

        ((CustomActivity)getContext()).setContinueButtonEnabled(true);
        ((CustomActivity)getContext()).setStartButtonEnabled(false);
        m_currentTrail = 0;
    }

    public void startTrial() {
        m_trailStartTime = System.currentTimeMillis();
        m_currentTrail += 1;

        resetCounters();
        addBall();
    }

    public void endTrail() {
        long trailEndTime = System.currentTimeMillis();
        long timeElapse = trailEndTime - m_trailStartTime;

        if (m_currentBlock == 0) {
            ++m_currentBlock;
        }

        if (m_currentTrail == 0) {
            ++m_currentTrail;
        }

        //<participantID> <participantName> <condition> <block#> <trial#> <receiver name> <elapsed time for this trial> <number of errors for this trial> <number of release for this trial> <number of drops for this trial> <number of touch for this trial> <number of touch ball for this trial> <number of long press for this trial> <timestamp>
        if (m_logger != null) {
            m_logger.write(m_id + "," + m_name + "," + getResources().getString(R.string.app_name) + "," + m_currentBlock + "," + m_currentTrail + "," + m_receiverName + "," + timeElapse + "," + m_numberOfErrors + "," + m_numberOfRelease + "," + m_numberOfDrops + "," + m_numberOfTouch + "," + m_numberOfTouchBall + "," + m_numberOfLongPress + "," + trailEndTime, true);
        }

        if (m_angleLogger != null) {
            m_angleLogger.flush();
        }

        if (m_matrixLogger != null) {
            m_matrixLogger.flush();
        }

        if (m_currentTrail < m_maxTrails) {
            startTrial();
        } else {
            endBlock();
        }
    }

    public void closeLogger() {
        if (m_logger != null) {
            m_logger.close();
        }

        if (m_angleLogger != null) {
            m_angleLogger.close();
        }

        if (m_matrixLogger != null) {
            m_matrixLogger.flush();
        }
    }

    private void resetCounters() {
        m_numberOfDrops = 0;
        m_numberOfErrors = 0;
        m_numberOfTouch = 0;
        m_numberOfTouchBall = 0;
        m_numberOfLongPress = 0;
        m_numberOfRelease = 0;
        m_receiverName = "";
    }
    /**
     * experiment end
     */
}
