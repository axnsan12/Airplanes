package com.axnsan.airplanes.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;


public class RandomizedQueue<Item> implements Iterable<Item> {
    private Item[] items = null;
    private int size = 0;
    private static Random random = new Random();
    
    @SuppressWarnings("unchecked")
    public RandomizedQueue() {
        items = (Item[]) new Object[1];
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    /**Double the array's capacity*/
    @SuppressWarnings("unchecked")
    private void doubleCapacity() {
        if (items.length == 0) {
            items = (Item[]) new Object[1];
            return;
        }
        Item[] tmp = (Item[]) new Object[items.length*2];
        for (int i = 0;i < size;++i) {
                tmp[i] = items[i];
            items[i] = null;
        }
        items = tmp;
    }
    
    /**Halve the array's capacity and clear the garbage*/
    @SuppressWarnings("unchecked")
    private void halveCapacity() {
        if (size > items.length/2)
            throw new RuntimeException("Cannot halve the array if it is more than half filled");
        
        Item[] tmp = (Item[]) new Object[items.length/2];
        for (int i = 0;i < size;++i) {
            if (items[i] != null)
                tmp[i] = items[i];
            items[i] = null;
        }
        items = tmp;
    }
    
    /**Add an item*/
    public void push(Item item) {
        if (item == null)
            throw new NullPointerException();
        
        if (size >= items.length)
            doubleCapacity();
        
        items[size++] = item;
    }
    
    /**Remove and return a random item*/
    public Item pop() {
        if (isEmpty())
            throw new NoSuchElementException();
        
        int index = random.nextInt(size);
        Item ret = items[index];
        items[index] = items[size-1];
        items[size-1] = null;
        --size;
        
        if (size <= items.length/4)
            halveCapacity();
        
        return ret;
    }
    
    /**Return a random item without removing*/
    public Item peek() {
        if (isEmpty())
            throw new NoSuchElementException();
        
        return items[random.nextInt(size)];
    }
    
    private static void shuffle(int[] array)
    {
        int index, temp;
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    
    private class RandomIterator implements Iterator<Item> {
        private int[] sequence;
        private int pos = 0;
        
        public RandomIterator() {
            sequence = new int[size];
            for (int i = 0;i < size;++i) {
                sequence[i] = i;
            }
            shuffle(sequence);
        }
        @Override
        public boolean hasNext() {
            return pos < sequence.length;
        }

        @Override
        public Item next() {
            if (!hasNext())
                throw new NoSuchElementException();
            
            return items[sequence[pos++]];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    /**Return an iterator in random order*/
    public Iterator<Item> iterator() {
        return new RandomIterator();
    }
}
