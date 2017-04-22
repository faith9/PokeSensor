package com.logickllc.pokemapper;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.logickllc.pokesensor.api.Account;
import com.logickllc.pokesensor.api.AccountManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class AccountsActivity extends AppCompatActivity {
    ListView list;
    public AccountsAdapter adapter;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        PokeFinderActivity.accountsActivityInstance = this;
        final Activity act = this;

        list = (ListView) findViewById(R.id.accountList);

        if (!AccountManager.accounts.isEmpty()) {
            initAdapter();
        } else {
            showEmptyMessage();
        }
        list.setLongClickable(true);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                // Show login screen
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle("Delete Account?");
                //builder.setMessage(R.string.loginMessage);
                builder.setMessage("Are you sure you want to delete " + AccountManager.accounts.get(position).getUsername() + "?");

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AccountManager.removeAccount(position);
                        reloadData();
                    }
                });
                builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing
                    }
                });

                try {
                    builder.create().show();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

        list.setClickable(true);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int index = position;
                if (index >= AccountManager.accounts.size()) return;

                Account account = AccountManager.accounts.get(index);

                switch (account.getStatus()) {
                    case CAPTCHA_REQUIRED:
                        account.go.setHasChallenge(false);
                        account.login(true);
                        break;

                    case WRONG_NAME_OR_PASSWORD:
                        AccountManager.showLoginScreen(account, act);
                        break;

                    case NEEDS_EMAIL_VERIFICATION:
                        PokeFinderActivity.features.longMessage("You need to verify your account using the activation email you received when you signed up.");
                        break;

                    case ERROR:
                        account.login();
                        break;

                    case BANNED:
                        PokeFinderActivity.features.longMessage("Niantic has banned your account from Pokemon Go. It will no longer work for scanning :(");
                        break;
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_add_account:
                AccountManager.showLoginScreen(null, this);
                return true;

            case R.id.action_import_accounts:
                importAccounts();
                return true;

            case R.id.action_export_accounts:
                exportAccounts();
                return true;

            case R.id.action_delete_accounts:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Delete All Accounts?").setMessage("Are you sure you want to delete all your accounts?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AccountManager.deleteAllAccounts();
                                showEmptyMessage();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                            }
                        });

                builder.create().show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.accounts_menu, menu);
        this.menu = menu;

        return true;
    }

    public void showEmptyMessage() {
        list.setVisibility(View.GONE);

        TextView emptyMessage = (TextView) findViewById(R.id.emptyAccountMessage);
        emptyMessage.setVisibility(View.VISIBLE);

        emptyMessage.setText("You don't have any accounts! Click + to add an account.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PokeFinderActivity.accountsActivityInstance = null;
    }

    public static void reloadData() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (PokeFinderActivity.accountsActivityInstance != null && PokeFinderActivity.accountsActivityInstance.adapter != null) PokeFinderActivity.accountsActivityInstance.adapter.notifyDataSetChanged();
            }
        };
        if (PokeFinderActivity.accountsActivityInstance != null) PokeFinderActivity.accountsActivityInstance.runOnUiThread(runnable);
    }

    public void importAccounts() {
        // Show login screen
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_import_accounts);
        final View view = getLayoutInflater().inflate(R.layout.import_accounts, null);
        builder.setView(view);
        final Activity act = this;

        builder.setPositiveButton("Import", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditText csv = (EditText) view.findViewById(R.id.csv);
                String csvText = csv.getText().toString().trim();

                if (csvText.equals("")) {
                    PokeFinderActivity.features.longMessage("You have to type at least 1 account/password combo before you can import...");
                    return;
                }

                try {
                    if (!csvText.equals("")) {
                        CSVParser parser;
                        if (csvText.contains(",")) parser = CSVParser.parse(csvText, CSVFormat.DEFAULT.withRecordSeparator("\n"));
                        else parser = CSVParser.parse(csvText, CSVFormat.DEFAULT.withRecordSeparator("\n").withDelimiter(' '));
                        for (CSVRecord record : parser) {
                            try {
                                String username = record.get(0).trim();
                                String password = record.get(1).trim();

                                if (username.equals("") || password.equals("")) continue;

                                // Adding a new account
                                Account newAccount = new Account(username, password, AccountManager.incNumAccounts(), act);

                                boolean dupe = false;

                                for (Account tempAccount : AccountManager.accounts) {
                                    if (tempAccount.getUsername().equals(newAccount.getUsername())) {
                                        AccountManager.decNumAccounts();
                                        dupe = true;
                                        break;
                                    }
                                }

                                if (dupe) continue;

                                newAccount.login();
                                AccountManager.accounts.add(newAccount);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AccountsActivity.reloadData();
            }
        });
        builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing
            }
        });

        try {
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportAccounts() {
        String accountString = "";
        for (Account account : AccountManager.accounts) {
            accountString += account.getUsername() + "," + account.getPassword() + "\n";
        }
        shareText(accountString);
    }

    public void shareText(String text) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);

        this.startActivity(Intent.createChooser(shareIntent, "Choose how to share"));
    }

    public void initAdapter() {
        adapter = new AccountsAdapter(this);
        list.setAdapter(adapter);
    }
}
