package com.logickllc.pokemapper;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.logickllc.pokesensor.api.ExceptionCatchingRunnable;
import com.logickllc.pokesensor.api.ExceptionCatchingThreadFactory;
import com.logickllc.pokesensor.api.Features;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.Signature;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import POGOProtos.Enums.PokemonIdOuterClass;

public class CustomImagesActivity extends AppCompatActivity {
    ListView list;
    CustomImagesAdapter adapter;
    Menu menu;
    private int counter = 0;
    private int fetchingUrls = 0;
    private ThreadPoolExecutor pool;
    private final int MAX_POOL_THREADS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_images);

        list = (ListView) findViewById(R.id.customImagesList);
        final Activity act = this;

        adapter = new CustomImagesAdapter(this);

        list.setLongClickable(true);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle("Delete Custom Image?");
                builder.setMessage("Are you sure you want to delete the image for " + PokeFinderActivity.mapHelper.getLocalName(position + 1) + "?");

                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PokeFinderActivity.features.customImages.remove(position);
                        PokeFinderActivity.features.customImages.add(position, " ");
                        PokeFinderActivity.features.saveCustomImagesUrls();

                        new File(PokeFinderActivity.features.getFilesRoot() + AndroidFeatures.CUSTOM_IMAGES_FOLDER + (position + 1) + PokeFinderActivity.mapHelper.IMAGE_EXTENSION).delete();
                        //resetImage(position + 1);
                        adapter.notifyDataSetChanged();
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
            public void onItemClick(AdapterView<?> parent, View view2, int position, long id) {
                final int row = position;
                
                if (fetchingUrls > 0) {
                    PokeFinderActivity.features.longMessage("Already fetching images. Please wait until they finish before trying again");
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                View view = act.getLayoutInflater().inflate(R.layout.input_box, null);

                TextView message = (TextView) view.findViewById(R.id.message);
                final EditText input = (EditText) view.findViewById(R.id.input);

                message.setText("Enter the URL for your custom image. Don't use a copyrighted image unless you have the rights to use it.");
                String defaultText = PokeFinderActivity.features.customImages.get(row);
                if (defaultText.equals(" ")) defaultText = "";
                input.setText(defaultText);
                //input.selectAll();

                builder.setTitle("Enter Image URL")
                        .setView(view)
                        .setPositiveButton("Fetch", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String text = input.getText().toString();
                                text = text.trim();
                                
                                if (text != null && !text.equals("") && text.indexOf("http") == 0) {
                                    PokeFinderActivity.features.customImages.remove(row);
                                    PokeFinderActivity.features.customImages.add(row, text);
                                    PokeFinderActivity.features.saveCustomImagesUrls();

                                    new File(PokeFinderActivity.features.getFilesRoot() + AndroidFeatures.CUSTOM_IMAGES_FOLDER + (row + 1) + PokeFinderActivity.mapHelper.IMAGE_EXTENSION).delete();

                                    counter = 0;
                                    fetchingUrls = 1;
                                    fetchImageFromUrl(text, row + 1);
                                } else {
                                    PokeFinderActivity.features.longMessage("Invalid image URL. Must start with http or https and be a valid image URL.");
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        });
                builder.create().show();
            }
        });

        list.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_import_custom_images:
                ArrayList<String> options = new ArrayList<String>();
                options.add("Website URL");
                options.add("Import URLs");

                ArrayList<Lambda> functions = new ArrayList<Lambda>();
                functions.add(new Lambda() {
                    @Override
                    public void execute() {
                        importCustomImages();
                    }
                });

                functions.add(new Lambda() {
                    @Override
                    public void execute() {
                        importUrls();
                    }
                });

                AndroidFeatures.showNativeOptionsList(options, functions, this);
                return true;

            case R.id.action_export_custom_images:
                exportCustomImages();
                return true;

            case R.id.action_delete_custom_images:
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Delete All?");
                    builder.setMessage("Are you sure you want to delete all custom images?");

                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PokeFinderActivity.features.customImages.clear();
                            for (int n = 0; n < Features.NUM_POKEMON; n++) {
                                PokeFinderActivity.features.customImages.add(" ");
                            }
                            PokeFinderActivity.features.saveCustomImagesUrls();

                            try {
                                FileUtils.deleteDirectory(new File(PokeFinderActivity.features.getFilesRoot() + AndroidFeatures.CUSTOM_IMAGES_FOLDER));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            adapter.notifyDataSetChanged();
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void importUrls() {
        if (fetchingUrls > 0) {
            PokeFinderActivity.features.longMessage("Already fetching images. Please wait until they finish before trying again");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = this.getLayoutInflater().inflate(R.layout.import_accounts, null);

        TextView message = (TextView) view.findViewById(R.id.importMessage);
        final EditText input = (EditText) view.findViewById(R.id.csv);

        message.setText("Enter the list of image URLs that you want to use. Each URL should be separated by a comma. Don't use copyrighted images unless you have the rights to use them.");
        input.setText("");

        builder.setTitle("Enter Image URLs")
                .setView(view)
                .setPositiveButton("Fetch", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int cursor = 0;
                        String fullText = input.getText().toString();

                        counter = 0;
                        fetchingUrls = fullText.split(",").length;

                        for (String text : fullText.split(",")) {
                            if (cursor >= Features.NUM_POKEMON) break;
                            text = text.trim();
                            if (text != null && !text.equals("") && text.indexOf("http") == 0) {
                                PokeFinderActivity.features.customImages.remove(cursor);
                                PokeFinderActivity.features.customImages.add(cursor, text);
                                PokeFinderActivity.features.saveCustomImagesUrls();

                                new File(PokeFinderActivity.features.getFilesRoot() + AndroidFeatures.CUSTOM_IMAGES_FOLDER + (cursor + 1) + PokeFinderActivity.mapHelper.IMAGE_EXTENSION).delete();

                                fetchImageFromUrl(text, cursor + 1);
                            } else {
                                //MapController.features.longMessage("Invalid image URL. Must start with http or https and be a valid image URL.");
                            }
                            cursor++;
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
        builder.create().show();
    }

    public void importCustomImages() {
        if (fetchingUrls > 0) {
            PokeFinderActivity.features.longMessage("Already fetching images. Please wait until they finish before trying again");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = this.getLayoutInflater().inflate(R.layout.input_box, null);

        TextView message = (TextView) view.findViewById(R.id.message);
        final EditText input = (EditText) view.findViewById(R.id.input);

        message.setText("Enter the base URL for your custom images. The app will do the rest. Don't use copyrighted images unless you have the rights to use them.");
        input.setText("");

        builder.setTitle("Enter Image URL")
                .setView(view)
                .setPositiveButton("Fetch", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = input.getText().toString();
                        text = text.trim();
                        if (text != null && !text.equals("") && text.indexOf("http") == 0 && text.lastIndexOf("/") == text.length() - 1) {
                            fetchAllImagesFromUrl(text);
                        } else {
                            PokeFinderActivity.features.longMessage("Invalid image URL. Must start with http or https and end with /");
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
        builder.create().show();
    }

    public void fetchImageFromUrl(final String url, final int pokedexNumber) {
        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        new File(AndroidFeatures.CUSTOM_IMAGES_FOLDER).mkdirs();
                        File dest = new File(PokeFinderActivity.features.getFilesRoot() + Features.CUSTOM_IMAGES_FOLDER + pokedexNumber + PokeFinderActivity.mapHelper.IMAGE_EXTENSION);

                        FileUtils.copyURLToFile(new URL(url), dest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    incCounter();
                }
            };
            run(runnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fetchAllImagesFromUrl(final String url) {
        counter = 0;
        fetchingUrls = Features.NUM_POKEMON;

        ArrayList<String> urls = new ArrayList<>();
        for (int n = 1; n <= Features.NUM_POKEMON; n++) {
            String name = PokemonIdOuterClass.PokemonId.valueOf(n).name();
            name = name.toLowerCase().replaceAll("\\.","").replaceAll("'","").replaceAll("_female","f").replaceAll("_male","m").replaceAll(" ","-").replaceAll("_","-").replaceAll("♂","m").replaceAll("♀","f");
            urls.add(url + name + PokeFinderActivity.mapHelper.IMAGE_EXTENSION);
        }

        PokeFinderActivity.features.customImages = urls;
        PokeFinderActivity.features.saveCustomImagesUrls();

        try {
            FileUtils.deleteDirectory(new File(AndroidFeatures.CUSTOM_IMAGES_FOLDER));
        } catch (Exception e) {
            e.printStackTrace();
        }

        reloadData();

        for (int n = 0; n < Features.NUM_POKEMON; n++) {
            fetchImageFromUrl(urls.get(n), n+1);
        }
    }

    public void exportCustomImages() {
        String bigString = "";

        for (int n = 0; n < Features.NUM_POKEMON; n++) {
            bigString += PokeFinderActivity.features.customImages.get(n) + (n == Features.NUM_POKEMON - 1 ? "" : ",");
        }

        shareText(bigString);
    }

    public void shareText(String text) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);

        this.startActivity(Intent.createChooser(shareIntent, "Choose how to share"));
    }

    private synchronized void incCounter() {
        counter++;
        if (counter == fetchingUrls || counter % 10 == 0) reloadData();
        if (counter == fetchingUrls) {
            counter = 0;
            fetchingUrls = 0;
        }
    }

    public synchronized Future run(Runnable runnable) {
        if (pool == null) {
            PokeFinderActivity.features.print("PokeFinder", "Initializing a new thread pool");
            pool = new ThreadPoolExecutor(MAX_POOL_THREADS, MAX_POOL_THREADS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            pool.setThreadFactory(new ExceptionCatchingThreadFactory(pool.getThreadFactory()));
        }
        Future future = pool.submit(new ExceptionCatchingRunnable(runnable));
        if (PokeFinderActivity.IS_AD_TESTING) PokeFinderActivity.features.print("PokeFinder", pool.getQueue().toString());
        return future;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.custom_images_menu, menu);
        this.menu = menu;

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PokeFinderActivity.features.saveCustomImagesUrls();
    }

    public void reloadData() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        };
        runOnUiThread(runnable);
    }
}
