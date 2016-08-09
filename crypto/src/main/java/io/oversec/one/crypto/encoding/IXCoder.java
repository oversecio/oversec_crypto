package io.oversec.one.crypto.encoding;

import io.oversec.one.crypto.encoding.pad.AbstractPadder;
import io.oversec.one.crypto.proto.Outer;

public interface IXCoder {

    Outer.Msg decode(String encText) throws Exception;

    String encode(Outer.Msg msg, AbstractPadder padder, String plainTextForWidthCalculation, boolean appendNewLines) throws Exception;

    String getId();

    String getLabel(AbstractPadder padder);

    String getExample(AbstractPadder padder);

    boolean isTextOnly();
}
