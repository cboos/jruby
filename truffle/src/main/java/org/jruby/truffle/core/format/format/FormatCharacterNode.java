package org.jruby.truffle.core.format.format;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.LiteralFormatNode;
import org.jruby.truffle.core.format.convert.ToIntegerNode;
import org.jruby.truffle.core.format.convert.ToIntegerNodeGen;
import org.jruby.truffle.core.format.convert.ToStringNode;
import org.jruby.truffle.core.format.convert.ToStringNodeGen;
import org.jruby.truffle.core.format.exceptions.NoImplicitConversionException;
import org.jruby.truffle.core.format.write.bytes.WriteByteNodeGen;
import org.jruby.truffle.language.control.RaiseException;

import java.nio.charset.StandardCharsets;

@NodeChildren({
    @NodeChild(value = "width", type = FormatNode.class),
    @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class FormatCharacterNode extends FormatNode {

    private final boolean hasMinusFlag;

    @Child private ToIntegerNode toIntegerNode;
    @Child private ToStringNode toStringNode;

    public FormatCharacterNode(RubyContext context, boolean hasMinusFlag) {
        super(context);
        this.hasMinusFlag = hasMinusFlag;
    }

//    @TruffleBoundary
    @Specialization
    protected byte[] format(VirtualFrame frame, int width, Object value) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(ToStringNodeGen.create(getContext(),
                false,
                "to_str",
                false,
                null,
                WriteByteNodeGen.create(getContext(), new LiteralFormatNode(getContext(), value))));
        }
        Object toStrResult;
        try {
            toStrResult = toStringNode.executeToString(frame, value);
        } catch (NoImplicitConversionException e) {
            toStrResult = null;
        }

        final String charString;
        if (toStrResult == null || isNil(toStrResult)) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(ToIntegerNodeGen.create(getContext(), null));
            }
            final int charValue = (int) toIntegerNode.executeToInteger(frame, value);
            // TODO BJF check char length is > 0
            charString = Character.toString((char) charValue);
        } else {
            final String resultString = new String((byte[]) toStrResult);
            final int size = resultString.length();
            if (size > 1) {
                throw new RaiseException(getContext().getCoreExceptions().argumentError("%c requires a character", this));
            }
            charString = resultString;
        }


        final boolean leftJustified = hasMinusFlag || width < 0;
        if (width < 0) {
            width = -width;
        }

        final String result = String.format("%" + (leftJustified ? "-" : "") + width + "." + width + "s", charString);
        return result.getBytes(StandardCharsets.US_ASCII);
    }

}
