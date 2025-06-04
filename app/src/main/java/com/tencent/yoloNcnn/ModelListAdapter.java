package com.tencent.yoloNcnn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelListAdapter extends RecyclerView.Adapter<ModelListAdapter.ButtonViewHolder> {

    public ModelListAdapter(List<String> items) {
        Param.checkedStates = new ArrayList<>(Collections.nCopies(items.size(), true));
    }

    @Override
    public ButtonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_view, parent, false);
        return new ButtonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ButtonViewHolder holder, int position) {
        String modelName = MainActivity.modelData.get(position);
        holder.modelName.setText(modelName);
        holder.del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ("ncnn".equals(Param.推理框架)) {
                    File file = new File(Param.ncnn文件地址 + Param.yolo版本 + "/" + modelName + ".bin");
                    file.delete();
                    file = new File(Param.ncnn文件地址 + Param.yolo版本 + "/" + modelName + ".param");
                    file.delete();
                    file = new File(Param.ncnn文件地址 + Param.yolo版本 + "/" + modelName + ".class");
                    file.delete();
                } else if ("tflite".equals(Param.推理框架)) {
                    File file = new File(Param.tflite文件地址 + modelName + ".tflite");
                    file.delete();
                    file = new File(Param.tflite文件地址 + modelName + ".class");
                    file.delete();
                }
                MainActivity.刷新模型列表();
            }
        });
    }

    @Override
    public int getItemCount() {
        return MainActivity.modelData.size();
    }


    static class ButtonViewHolder extends RecyclerView.ViewHolder {
        TextView modelName;
        Button del;

        public ButtonViewHolder(View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.modelId);
            del = itemView.findViewById(R.id.删除模型);
        }
    }
}
