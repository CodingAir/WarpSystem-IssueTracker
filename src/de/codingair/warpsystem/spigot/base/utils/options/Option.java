package de.codingair.warpsystem.spigot.base.utils.options;

import java.util.Objects;
import java.util.function.Predicate;

public class Option<E> {
    private String path;
    private E value;
    private E def;
    private State state;
    private Predicate<E> predicate = null;

    public Option(String path) {
        this.path = path;
        this.state = State.UNLOADED;
    }

    public Option(String path, E def) {
        this.path = path;
        this.def = def;
        this.state = State.UNLOADED;
    }

    public Option(String path, E def, Predicate<E> predicate) {
        this.path = path;
        this.def = def;
        this.state = State.UNLOADED;
        this.predicate = predicate;
    }

    public Option(String path, E value, E def) {
        this.path = path;
        this.value = value;
        this.def = def;
        this.state = State.LOADED;
    }

    public String getPath() {
        return path;
    }

    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        if(state == State.UNLOADED) {
            if(predicate != null) {
                if(predicate.test(value)) {
                    this.value = value;
                    state = State.LOADED;
                } else {
                    this.value = def;
                    state = State.CHANGED;
                }
            } else {
                this.value = value;
                state = State.LOADED;
            }
        } else if(!Objects.equals(this.value, value)) {
            this.value = value;
            state = State.CHANGED;
        }
    }

    public boolean hasChanged() {
        return state == State.CHANGED;
    }

    public Option<E> clone() {
        Option o = new Option<>(path, value, def);
        o.state = this.state;
        return o;
    }

    public E getDefault() {
        return def;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Option) {
            Option o = (Option) obj;
            return o.getPath().equals(path) && Objects.equals(getValue(), o.getValue()) && Objects.equals(getDefault(), o.getDefault());
        } else return false;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    private enum State {
        UNLOADED,
        LOADED,
        CHANGED
    }
}
