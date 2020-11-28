import javax.crypto.SecretKey;

public class WriterThread extends Thread {

    WriteHelper writer;
    Server server;

    public WriterThread(Server server, SecretKey encryptKey) {
        this.server = server;
        writer = new WriteHelper(encryptKey);
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            try{
                Thread.sleep(500);
                writer.writeAllData(server.getUserMap());
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
