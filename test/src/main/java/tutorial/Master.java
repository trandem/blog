package tutorial;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Random;

public class Master implements Watcher {
    private String hostPort;
    private ZooKeeper zk;

    private Random random = new Random(this.hashCode());
    String serverId = Integer.toHexString(random.nextInt());

    enum MasterStates {RUNNING, ELECTED, NOT_ELECTED};

    private volatile MasterStates state = MasterStates.RUNNING;

    public Master(String hostPort) {
        this.hostPort = hostPort;
    }

    public void startZoo() throws IOException {
        System.out.println("ThreadID start "+ Thread.currentThread().getId());
        zk = new ZooKeeper(hostPort, 1000, this);
    }

    public void stopZoo() throws InterruptedException {
        zk.close();
    }

    public void masterElection() {
        this.zk.create("/master", serverId.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, createMasterCallback, null);
    }

    AsyncCallback.Create2Callback createMasterCallback = (rc, path, ctx, name, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case OK:
                state = MasterStates.ELECTED;
                System.out.println("I am a master");
                break;
            case CONNECTIONLOSS:
                checkMaster();
                break;
            case NODEEXISTS:
                state = MasterStates.NOT_ELECTED;
                masterExist();
                break;
            default:
                break;
        }
    };

    private void checkMaster() {
        this.zk.getData("/master", false, checkMasterCallBack, null);
    }

    private AsyncCallback.DataCallback checkMasterCallBack = (rc, path, ctx, data, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case NONODE:
                masterElection();
                break;
            case OK:
                if (serverId.equals(new String(data))) {
                    state = MasterStates.ELECTED;
                } else {
                    state = MasterStates.NOT_ELECTED;
                    masterExist();
                }
                break;
            case CONNECTIONLOSS:
                checkMaster();
            default:
                System.out.println("error checkMasterCallBack");
                break;
        }
    };

    private void masterExist() {
        this.zk.exists("/master", this::existWatcher, existCallBack, null);
    }

    AsyncCallback.StatCallback existCallBack = (rc, path, ctx, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case OK:
                break;
            case CONNECTIONLOSS:
                masterExist();
                break;
            case NONODE:
                state = MasterStates.RUNNING;
                masterElection();
                System.out.println("It sounds like the previous master is gone, so let's run for master again.");
                break;
            default:
                System.out.println("error existCallBack");
                break;
        }
    };

    public void existWatcher(WatchedEvent event) {
        if (event.getType().equals(Event.EventType.NodeDeleted)) {
            assert "/master".equals(event.getPath());
            masterExist();
        }
    }


    @Override
    public void process(WatchedEvent event) {
        System.out.println("ThreadIDConnect "+ Thread.currentThread().getId());
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected:
                    System.out.println("connected");
                    break;
                case Disconnected:
                    System.out.println("Disconnected");
                    break;
                case Expired:
                    System.out.println("Expired");
                default:
                    break;
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Master master = new Master("localhost:2181");
        master.startZoo();

        master.masterElection();
        while (true) {

        }
    }
}
