package org.testtask;

public interface Storage {

    /**
     * Should store only unique numbers
     *
     * @param number - number to store (note, will throw exception, if not valid number)
     */
    void add(String number);
}
