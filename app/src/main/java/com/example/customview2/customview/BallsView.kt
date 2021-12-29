package com.example.balls.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.example.customview2.customview.MyBall
import com.example.customview2.R
import kotlinx.coroutines.*
import kotlin.math.abs

class BallsView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_RADIUS = 30F
        private const val TIME_TO_RETURN = 200L
        private const val TIME_DELAY_AUTO = 300L
        private const val DELTA_AUTO = 10
        private const val SUM_BALLS_DEFAULT = 5
        private val listColor = mutableListOf(
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            Color.YELLOW
        )
    }
    private var listBalls: MutableList<MyBall> = mutableListOf()
    private lateinit var ballPaint: Paint
    private var radius = DEFAULT_RADIUS
    var sumBall = SUM_BALLS_DEFAULT
    private var initCoordinatesX = 0
    private var initCoordinatesY = 0
    private var isAuto = true
    private var isBack = false

    init {
        context?.theme?.obtainStyledAttributes(
            attrs,
            R.styleable.ballbounce,
            0, 0
        )?.apply {
            try {
                sumBall = getInt(R.styleable.ballbounce_sumBall, SUM_BALLS_DEFAULT)
            } catch (e: Exception) {
                e.stackTrace
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        initCoordinatesX = widthSpecSize
        initCoordinatesY = heightSpecSize
        initBalls()
        setMeasuredDimension(widthSpecSize, heightSpecSize)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawListBall(canvas)
        autoUpAndDown()
        invalidate()
    }

    private fun initBalls() {
        listBalls.clear()
        for (i in sumBall - 1 downTo 0) { // ve tu phai sang
            val x = (2 * i + 1) * (initCoordinatesX / (2 * sumBall)).toFloat()
            val y = initCoordinatesY / 2.toFloat()
            val ball = MyBall(x, y)
            listBalls.add(ball)
        }
    }

    private fun drawListBall(canvas: Canvas?) {
        ballPaint = Paint()
        for (i in 0 until listBalls.size) {
            with(ballPaint) {
                color = listColor[i % 4]
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            canvas?.drawCircle(listBalls[i].x, listBalls[i].y, radius, ballPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                jobBack?.cancel()
                jobAuto?.cancel()
                isAuto = false
                isBack = false
                handler.removeCallbacksAndMessages(DelayThread())
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in listBalls.size - 1 downTo 1) {
                    listBalls[i].x = listBalls[i - 1].x
                    listBalls[i].y = listBalls[i - 1].y
                }
                listBalls[0].x = event.x
                listBalls[0].y = event.y
            }

            MotionEvent.ACTION_UP -> {
                isBack = true
                for (i in 0 until listBalls.size) {
                    val x = initCoordinatesX - (2 * i + 1) * (initCoordinatesX / (2 * sumBall)).toFloat()
                    val y = initCoordinatesY / 2.toFloat()
                    ballMoveBack(i, x, y)
                }
                handler.postDelayed(DelayThread(), 3000)
            }
        }
        return true
    }

    private fun autoUpAndDown() {
        val ball = listBalls[0]
        if (isAuto) {
            if (ball.isUp) {
                ball.y -= DELTA_AUTO
            } else {
                ball.y += DELTA_AUTO
            }
            if (abs(ball.y - initCoordinatesY / 3.toFloat()) <= DELTA_AUTO)
                ball.isUp = false
            if (abs(ball.y - 2 * initCoordinatesY / 3) <= DELTA_AUTO)
                ball.isUp = true
        }
        for (i in 1 until listBalls.size) {
            val y = listBalls[i - 1].y
            moveBall(i, y)
        }
    }

    private var jobAuto: Job? = null
    private fun moveBall(i: Int, newY: Float) {
        val ball = listBalls[i]
        jobAuto = CoroutineScope(Dispatchers.Default).launch {
            delay(TIME_DELAY_AUTO)
            if (isAuto && isActive) {
                ball.y = newY
            }
        }
    }
//    private fun autoUpAndDown() {
//        for (i in 0 until listBalls.size) {
//            val ball = listBalls[i]
//            jobAuto = CoroutineScope(Dispatchers.Default).launch {
//                delay(TIME_DELAY_AUTO * i)
//                if (isAuto && isActive) {
//                    if (ball.isUp) {
//                        ball.y -= DELTA_AUTO
//                    } else {
//                        ball.y += DELTA_AUTO
//                    }
//                    if (abs(ball.y - initCoordinatesY / 3.toFloat()) <= DELTA_AUTO)
//                        ball.isUp = false
//                    if (abs(ball.y - 2 * initCoordinatesY / 3) <= DELTA_AUTO)
//                        ball.isUp = true
//                }
//            }
//        }
//    }

    private var jobBack: Job? = null
    private fun ballMoveBack(i: Int, newX: Float, newY: Float) {
        jobBack = CoroutineScope(Dispatchers.Default).launch {
            delay(TIME_TO_RETURN * i + 3000)
            val ball = listBalls[i]
            var isDone = false
            while (!isDone && isActive && isBack) {
                delay(2)
                if (ball.x - newX > 1)
                    ball.x--
                if (ball.x - newX < -1)
                    ball.x++
                if (ball.y - newY > 1)
                    ball.y--
                if (ball.y - newY < -1)
                    ball.y++
                if (abs(ball.x - newX) <= 1 && abs(ball.y - newY) <= 1) {
                    isDone = true
                    ball.x = newX
                    ball.y = newY
                }
            }
        }
    }

    inner class DelayThread : Runnable {
        override fun run() {
            val x = initCoordinatesX - (2 * (sumBall - 1) + 1) * (initCoordinatesX / (2 * sumBall)).toFloat()
            val y = initCoordinatesY / 2.toFloat()
            if (listBalls.last().x == x && listBalls.last().y == y) {
                for (ball in listBalls) {
                    ball.isUp = true
                }
                isAuto = true
            } else post(DelayThread())
        }
    }
}