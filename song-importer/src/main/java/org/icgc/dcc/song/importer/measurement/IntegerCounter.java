package org.icgc.dcc.song.importer.measurement;

import java.util.Collection;

public class IntegerCounter implements Countable<Integer> {

  private static final int DEFAULT_INIT_VAL = 0;

  private final int initVal;
  private int count;

  public IntegerCounter(final int initVal) {
    this.initVal = initVal;
    this.count = initVal;
  }

  @Override
  public void incr() {
    count++;
  }

  @Override
  public void incr(final Integer amount) {
    count += amount;
  }

  @Override
  public void reset() {
    count = initVal;
  }

  @Override
  public Integer getCount() {
    return count;
  }

  @Override
  public <C extends Collection<?>> C streamCollectionCount(C objects) {
    this.incr(objects.size());
    return objects;
  }

  public static IntegerCounter newIntegerCounter(final int initVal){
    return new IntegerCounter(initVal);
  }

  public static IntegerCounter newDefaultIntegerCounter(){
    return newIntegerCounter(DEFAULT_INIT_VAL);
  }


}