package com.mirim.a3mcoding.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.mirim.a3mcoding.R
import com.mirim.a3mcoding.adapter.ProblemAnswerAdapter
import com.mirim.a3mcoding.databinding.ActivityProblemBinding
import com.mirim.a3mcoding.model.LevelProblem
import com.mirim.a3mcoding.model.Problem
import com.mirim.a3mcoding.model.StageProblem
import com.mirim.a3mcoding.model.app
import com.mirim.a3mcoding.network.RetrofitClient
import com.mirim.a3mcoding.server.request.StageSolveRequest
import com.mirim.a3mcoding.server.response.LevelProblemResponse
import com.mirim.a3mcoding.server.response.StageListResponse
import com.mirim.a3mcoding.server.response.StageProblemResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class ProblemActivity : AppCompatActivity() {
    lateinit var binding: ActivityProblemBinding
    var problemType:String?= ""
    var answerList = HashMap<Int, String>()
    lateinit var answers: List<String>
    var correct = true
    var type = "0"
    var no : Int? = 0
    var id: Int?  = 0
    var level: String? = ""
    var time: Int? = 0
    var desc_path:String? = ""

    companion object {
        val TAG = "ProblemActivity"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        problemType = intent.getStringExtra("problemType")
        no = intent.getIntExtra("no", 0)
        id = intent.getIntExtra("id", 0)
        level = intent.getStringExtra("level")
        time = intent.getIntExtra("time", 1)

        when(problemType) {
            "stage" -> getStageProblem()
            "level" -> getLevelProblem()
            "recommendation" -> getRecommendationProblem()
        }

        val adapter = ArrayAdapter.createFromResource(applicationContext, R.array.language, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter


        binding.btnSubmit.setOnClickListener {
            correct = true
            Log.d(TAG, answerList.toString())
            Log.d(TAG, answers.toString())
            for(i in 0 until answers.size) {
                if(answerList.get(i) != answers[i]) {
                    correct = false
                    break
                }
            }
            if(correct) {
                Toast.makeText(applicationContext, "정답입니다.", Toast.LENGTH_SHORT).show()
                binding.btnDescription.visibility = View.VISIBLE
                binding.btnSubmit.visibility = View.INVISIBLE
                app.user.solve_count = app.user.solve_count!! + 1
                app.user.stage = app.user.stage!! + 1
                solveStage();
            }
            else Toast.makeText(applicationContext, "틀렸습니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnDescription.setOnClickListener {
            val intent = Intent(applicationContext, DescriptionActivity::class.java)
            intent.putExtra("title", binding.toolbar.textTitle.text)
            intent.putExtra("problemType", problemType)
            intent.putExtra("desc_path", desc_path)
            startActivity(intent)
            finish()
        }

        binding.spinnerLanguage.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                type = StageProblem.typeNumberConverter(binding.spinnerLanguage.getItemAtPosition(p2).toString())
                Log.d("spinnerLanguage", type)
                getStageProblem()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        })

    }
    fun getStageProblem() {
        RetrofitClient.serviceAPI.getStageProblem(no, type).enqueue(object : Callback<StageProblemResponse> {
            override fun onResponse(
                call: Call<StageProblemResponse>,
                response: Response<StageProblemResponse>
            ) {
                Log.d(TAG, response.body().toString())
                if(response.raw().code() == 200) {
                    val stageProblem = response.body()?.data
                    desc_path = stageProblem?.desc_path
                    binding.toolbar.textTitle.text = ""+stageProblem?.no + "번 - " + stageProblem?.title
                    binding.txtQuestion.text = stageProblem?.question
                    binding.txtPrint.text = stageProblem?.print
                    binding.txtCode.text = stageProblem?.code
                    binding.recyclerAnswer.layoutManager = object : LinearLayoutManager(applicationContext){ override fun canScrollVertically(): Boolean { return false } }
                    binding.recyclerAnswer.adapter = ProblemAnswerAdapter(applicationContext, stageProblem?.answer_count, answerList)
                    answers = stageProblem?.answers!!.split(".")
                    Log.d(TAG+"answers", answers.toString())
                }
                else {
                    Toast.makeText(applicationContext, response.raw().message(), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<StageProblemResponse>, t: Throwable) {
                Log.d(TAG+" fail", t.toString())
            }

        })
    }

    fun getLevelProblem() {
        RetrofitClient.serviceAPI.getLevelProblem(id).enqueue(object : Callback<LevelProblemResponse> {
            override fun onResponse(
                call: Call<LevelProblemResponse>,
                response: Response<LevelProblemResponse>
            ) {
                Log.d(TAG, response.body().toString())
                if(response.raw().code() == 200) {
                    val levelProblem = response.body()?.data
                    desc_path = levelProblem?.desc_path
                    binding.toolbar.textTitle.text = levelProblem?.title + " " + LevelProblem.levelKoreanConverter(levelProblem?.level)
                    binding.txtQuestion.text = levelProblem?.question
                    binding.txtPrint.text = levelProblem?.print
                    binding.txtCode.text = levelProblem?.code
                    binding.recyclerAnswer.layoutManager = LinearLayoutManager(applicationContext)
                    binding.recyclerAnswer.adapter = ProblemAnswerAdapter(applicationContext, levelProblem?.answer_count, answerList)
                    answers = levelProblem?.answers!!.split(".")
                }
            }

            override fun onFailure(call: Call<LevelProblemResponse>, t: Throwable) {
                Log.d(TAG, t.toString())
            }

        })
    }

    fun getRecommendationProblem() {
        RetrofitClient.serviceAPI.getRecommendation(level, time).enqueue(object : Callback<LevelProblemResponse> {
            override fun onResponse(
                call: Call<LevelProblemResponse>,
                response: Response<LevelProblemResponse>
            ) {
                Log.d(TAG, response.body().toString())
                val levelProblem = response.body()?.data
                desc_path = levelProblem?.desc_path
                binding.toolbar.textTitle.text = levelProblem?.title + " " + LevelProblem.levelKoreanConverter(levelProblem?.level)
                binding.txtQuestion.text = levelProblem?.question
                binding.txtPrint.text = levelProblem?.print
                binding.txtCode.text = levelProblem?.code
                binding.recyclerAnswer.layoutManager = LinearLayoutManager(applicationContext)
                binding.recyclerAnswer.adapter = ProblemAnswerAdapter(applicationContext, levelProblem?.answer_count, answerList)
                answers = levelProblem?.answers!!.split(".")
            }

            override fun onFailure(call: Call<LevelProblemResponse>, t: Throwable) {
                Log.d(TAG, t.toString())
            }

        })
    }

    fun solveStage() {
        RetrofitClient.serviceAPI.solveStage(StageSolveRequest(app.user.email)).enqueue(object : Callback<StageListResponse> {
            override fun onResponse(
                call: Call<StageListResponse>,
                response: Response<StageListResponse>
            ) {
                Log.d(TAG+"solve", response.toString())
            }

            override fun onFailure(call: Call<StageListResponse>, t: Throwable) {
                Log.d(TAG, t.toString())
            }

        })
    }
}