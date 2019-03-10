package makholin;

class Entity {
    private long id;
    private long counter;

    Entity(long id) {
        this.id = id;
    }

    long getId() {
        return id;
    }

    long getCounter() {
        return counter;
    }

    void increment() {
        this.counter = this.counter + 1;
    }
}
