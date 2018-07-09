package zig.tic.tac.adapter

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.task_layout.view.*
import zig.tic.App
import zig.tic.tac.R
import zig.tic.tac.activity.MainActivity
import zig.tic.tac.entity.Task
import zig.tic.tac.util.secondsToTime

class TaskAdapter(private val tasks: MutableList<Task>) : RecyclerView.Adapter<TaskAdapter.VH>() {

    companion object {
        val TAG: String = TaskAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(LayoutInflater.from(parent.context).inflate(R.layout.task_layout, parent, false))

    override fun getItemCount() = tasks.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = tasks[position]
        holder.itemView.tvTaskItemName.text = task.title
        holder.itemView.tvTaskItemTime.text = secondsToTime(task.getElapsedTime())
        holder.setTask(tasks[position])
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private lateinit var task: Task

        init {
            v.btnDeleteTask.setOnClickListener {
                (it.context.applicationContext as App).getBox().remove(task)
                tasks.remove(task)
                notifyItemRemoved(adapterPosition)
            }

            v.setOnClickListener {
                continueTask(it.context as MainActivity)
            }
        }

        private fun continueTask(mainActivity: MainActivity) {
            Log.i(TAG, "continue task")
            mainActivity.start(task.title, task)
        }

        fun setTask(task: Task) {
            this.task = task
        }
    }

}
