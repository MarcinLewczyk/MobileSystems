package com.example.marcinlewczyk.mobilesystem.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.marcinlewczyk.mobilesystem.POJO.DishInfo;
import com.example.marcinlewczyk.mobilesystem.R;

import java.util.List;

public class OrderProductsAdapter extends ArrayAdapter<DishInfo>{

    public OrderProductsAdapter(Context context, List<DishInfo> objects) {
        super(context, R.layout.double_list_element, objects);
    }

    private static class ViewHolder{
        TextView dishNameTextView;
        TextView dishQtyTextView;
    }

    @Override
    public View getView(int position, View converterView, ViewGroup parent){
        final DishInfo dishInfo = getItem(position);
        ViewHolder viewHolder;
        if(converterView == null){
            viewHolder = new ViewHolder();
            converterView = LayoutInflater.from(getContext()).inflate(R.layout.double_list_element, parent, false);
            viewHolder.dishNameTextView = converterView.findViewById(R.id.dishNameDoubleListElementLinearLayout);
            viewHolder.dishQtyTextView = converterView.findViewById(R.id.qtyDoubleListElementLinearLayout);
            converterView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) converterView.getTag();
        }
        viewHolder.dishNameTextView.setText(dishInfo.getDishName());
        viewHolder.dishQtyTextView.setText("" + dishInfo.getDishQty());
        return converterView;
    }
}