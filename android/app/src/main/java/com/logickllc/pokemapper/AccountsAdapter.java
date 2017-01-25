package com.logickllc.pokemapper;


import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.logickllc.pokesensor.api.Account;

import static com.logickllc.pokesensor.api.AccountManager.accounts;

public class AccountsAdapter extends BaseAdapter {
    Activity act;
    LayoutInflater inflater;

    public AccountsAdapter(Activity act) {
        this.act = act;
        inflater = act.getLayoutInflater();
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public Object getItem(int position) {
        return accounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.account_list_item, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.accountName);
            holder.status = (ImageView) convertView.findViewById(R.id.status);
            holder.statusLabel = (TextView) convertView.findViewById(R.id.accountStatus);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Account account = accounts.get(position);
        holder.name.setText(account.getUsername());

        holder.status.setVisibility(View.VISIBLE);

        holder.statusLabel.setText(account.getStatus().name().replaceAll("_", " "));

        switch (account.getStatus()) {
            case GOOD:
                holder.status.setImageResource(R.drawable.status_good);
                break;

            case CAPTCHA_REQUIRED:
                holder.status.setImageResource(R.drawable.status_warning);
                break;

            case BANNED:
                holder.status.setImageResource(R.drawable.status_banned);
                break;

            case INVALID_CREDENTIALS:
                holder.status.setImageResource(R.drawable.status_error);
                break;

            case NEEDS_EMAIL_VERIFICATION:
                holder.status.setImageResource(R.drawable.status_warning);
                break;

            case LOGGING_IN:
                holder.status.setVisibility(View.INVISIBLE);
                break;

            case SOLVING_CAPTCHA:
                holder.status.setVisibility(View.INVISIBLE);
                break;

            default:
                holder.status.setImageResource(R.drawable.status_error);
                break;
        }

        return convertView;
    }

    private static class ViewHolder {
        public TextView name;
        public ImageView status;
        public TextView statusLabel;
    }
}
