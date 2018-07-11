package com.devsmart.supernet;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.Random;
import java.util.UUID;

public class Main {

    private static class ConfigFile {
        String id;
        Integer port;
        String[] bootstrap;

    }

    static File gHomeDir;
    static ConfigFile gConfigFile;



    private static void readConfig() throws IOException  {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        File configFile = new File(gHomeDir, ".supernet");
        configFile = new File(configFile, "config.json");

        if(configFile.exists() && configFile.isFile()) {

            Reader reader = new FileReader(configFile);
            gConfigFile = gson.fromJson(reader, ConfigFile.class);
            reader.close();

        } else {
            configFile.getParentFile().mkdirs();

            ID id = ID.createRandom(new Random());

            gConfigFile = new ConfigFile();
            gConfigFile.id = id.toString(BaseEncoding.base16());

            FileWriter writer = new FileWriter(configFile);
            gson.toJson(gConfigFile, writer);
            writer.close();
        }

    }

    public static void main(String[] args) {

        try {

            String userHomeDir = System.getProperty("user.home");
            gHomeDir = new File(userHomeDir);

            readConfig();

            SupernetClient.Builder builder = new SupernetClient.Builder()
                    .withId(ID.fromString(gConfigFile.id, BaseEncoding.base16()));

            if(gConfigFile.port != null) {
                builder.withUDPPort(gConfigFile.port);
            }


            SupernetClient client = builder.build();

            client.start();

            Thread.sleep(1000);

            if(gConfigFile.bootstrap != null) {
                for(String address : gConfigFile.bootstrap) {
                    client.bootstrap(address);
                    Thread.sleep(500);
                }
            }

            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {

        }

    }
}
