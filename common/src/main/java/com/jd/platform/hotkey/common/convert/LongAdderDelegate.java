package com.jd.platform.hotkey.common.convert;

import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.WireFormat;
import io.protostuff.runtime.Delegate;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author qiujw wrote on 2021-06-29
 * @version 1.0
 */
public class LongAdderDelegate implements Delegate<LongAdder> {

    @Override
    public WireFormat.FieldType getFieldType() {
        return WireFormat.FieldType.INT64;
    }

    @Override
    public java.util.concurrent.atomic.LongAdder readFrom(Input input) throws IOException {
        java.util.concurrent.atomic.LongAdder cnt = new java.util.concurrent.atomic.LongAdder();
        cnt.add(input.readInt64());
        return cnt;
    }

    @Override
    public void writeTo(Output output, int number, LongAdder longAdder, boolean repeated) throws IOException {
        output.writeInt64(number, longAdder.sum(), repeated);
    }

    @Override
    public void transfer(Pipe pipe, Input input, Output output, int number,
                         boolean repeated) throws IOException {
        output.writeInt64(number, input.readInt64(), repeated);
    }

    @Override
    public Class<?> typeClass() {
        return java.util.concurrent.atomic.LongAdder.class;
    }
}
