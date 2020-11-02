public class WriterThread extends Thread {
    
    WriteHelper writer; 
    Server server;

    public WriterThread(Server server) {
        this.server = server;
        writer = new WriteHelper();
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            try{ 
                Thread.sleep(15000);
                writer.writeAllData(server.getUserMap());
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
