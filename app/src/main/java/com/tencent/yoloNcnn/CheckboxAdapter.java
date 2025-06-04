package com.tencent.yoloNcnn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckboxAdapter extends RecyclerView.Adapter<CheckboxAdapter.CheckboxViewHolder> {

    public CheckboxAdapter(List<String> items) {
        Param.checkedStates = new ArrayList<>(Collections.nCopies(items.size(), true));
    }

    public void setData(List<String> items){
        Param.checkedStates = new ArrayList<>(Collections.nCopies(items.size(), true));
    }

    @Override
    public CheckboxViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkbox, parent, false);
        return new CheckboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CheckboxViewHolder holder, int position) {
        String item = Param.yolo类名集合.get(position);
        holder.checkBox.setText(item);
        holder.checkBox.setChecked(Param.checkedStates.get(position));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Param.checkedStates.set(position, isChecked); // 更新选中状态
        });
    }

    @Override
    public int getItemCount() {
        return Param.yolo类名集合.size();
    }

    public List<Boolean> getCheckedStates() {
        return Param.checkedStates;
    }

    static class CheckboxViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        public CheckboxViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}
