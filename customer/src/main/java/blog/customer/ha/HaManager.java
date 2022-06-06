package blog.customer.ha;

import blog.common.glosory.LifeCycle;

public interface HaManager extends LifeCycle {

    Status getStatus();

    void addListener(Listener listener);

    void delListener(Listener listener);

    enum Status {
        MASTER, SLAVE;
    }


    interface Listener {
        void onMaster();

        void onSlave(Throwable th);
    }

}
