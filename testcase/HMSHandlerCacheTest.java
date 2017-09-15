import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStore;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class HMSHandlerCacheTest {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("args[0]:cpu cores, args[1]:task num, args[2]:read ratio, " +
                    "args[3]:ratio seed1, args[4]:row seed2, arg[5]:col seed3");
            return;
        }

        // initialization
        int cores = Integer.parseInt(args[0]);
        int tasknum = Integer.parseInt(args[1]);
        double readratio = Double.parseDouble(args[2]);
        Random r1 = new Random(Integer.parseInt(args[3]));
        Random r2 = new Random(Integer.parseInt(args[4]));
        CacheThread.r3 = new Random(Integer.parseInt(args[5]));

        ExecutorService ex = Executors.newFixedThreadPool(cores);

        FileReader fr;
        try {
            // select b.NAME,a.TBL_NAME from TBLS a join DBS b on a.DB_ID=b.DB_ID
            // into outfile '/var/lib/mysql-files/DBxTBL.csv' fields terminated by ',' lines terminated by '\n';
            fr = new FileReader("DBxTBL.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        BufferedReader br = new BufferedReader(fr);
        ArrayList<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int linesLen = lines.size();

        BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>();
        double readratioExpand = 2 * readratio - 1;
        for (int i = 0; i < tasknum; ++i) {
            boolean write = r1.nextDouble() < readratioExpand;
            String[] rstr = lines.get((int) r2.nextDouble() * linesLen).split(",");
            if (rstr.length != 2) {
                System.err.println("meta data format must be db,tbl");
                return;
            }
            bq.add(new CacheThread(write, rstr[0], rstr[1]));
        }

        // submit tasks
        Date start = new Date();
        while (!bq.isEmpty()) {
            ex.submit(bq.poll());
        }
        ex.shutdown();
        try {
            ex.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date end = new Date();

        // statistics
        double delta = (end.getTime() - start.getTime()) / 1000.0;
        double eval = tasknum / delta;
        System.out.println("Finished using " + delta + "s. ");
        System.out.println("Throughput is " + eval + "vps.");
    }

    static class CacheThread implements Runnable {

        public static Random r3 = new Random();

        private boolean writeBool = false;
        private String db = "default";
        private String tbl = "dual";

        CacheThread(boolean writeBool, String db, String tbl) {
            this.writeBool = writeBool;
            this.db = db;
            this.tbl = tbl;
        }

        private static HiveConf hiveConf = new HiveConf(HMSHandlerCacheTest.class);
        private static ThreadLocal<HiveMetaStore.HMSHandler> clients = new ThreadLocal<>();

        static {
            hiveConf.addResource("hive-site.xml");
        }

        @Override
        public void run() {
            try {
                HiveMetaStore.HMSHandler baseHandler = clients.get();
                if (baseHandler == null) {
                    baseHandler = new HiveMetaStore.HMSHandler("CacheTest", hiveConf, false);
                    clients.set(baseHandler);
                }

                Table tbl2 = baseHandler.get_table(db, tbl);
                if (writeBool) {
                    StorageDescriptor sd = tbl2.getSd();
                    List<FieldSchema> cols = sd.getCols();
                    int ridx = (int) (r3.nextDouble() * cols.size());
                    cols.get(ridx).setName("_cache_" + ridx);
                    baseHandler.alter_table(db, tbl, tbl2);
                }

            } catch (TException e) {
                e.printStackTrace();
            }
        }

    }

}
