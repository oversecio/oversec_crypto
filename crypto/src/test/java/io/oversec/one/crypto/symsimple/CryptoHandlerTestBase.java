package io.oversec.one.crypto.symsimple;

import android.content.Context;
import com.google.protobuf.ByteString;
import io.oversec.one.crypto.AbstractCryptoHandler;
import io.oversec.one.crypto.BuildConfig;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.symbase.KeyCache;
import io.oversec.one.crypto.symbase.KeyUtil;
import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public abstract class CryptoHandlerTestBase extends TestCase {

    static final String PLAIN_CONTENT = "Alan: did you enjoy singing beethoven? and in my hour of darkness baby lay down your arms don't forget me martha my dear drink my liquor een puur natuur kuur van een uur, van king arthur is iet zo duur i do all the pleasin' with you, it's so hard to reason with you i said 'even though you know what you know jingle bell, jingle bell, jingle bell rock just what i'm gonna do leave your flowers at my door; oh carol, don't let him on the heels and toes the minute you'll let her under your skin there are places i remember we have lost the time that was so hard to find well i said bye (bye bye bye bye) woo oo oo, woo yeah ah.";
    static final int INNER_PADDING_BYTES = 16;


    Context mContext;

    AbstractCryptoHandler mHandler;


    CryptoHandlerTestBase() {
        mContext = RuntimeEnvironment.application;
        mHandler = createHandler();
    }

    abstract AbstractCryptoHandler createHandler();


    Inner.InnerData createInnerData(String content) {
        Inner.InnerData.Builder innerDataBuilder = Inner.InnerData.newBuilder();

        Inner.TextAndPaddingV0.Builder textAndPaddingBuilder = innerDataBuilder.getTextAndPaddingV0Builder();

        textAndPaddingBuilder.setText(content);

        byte[] padding = KeyUtil.INSTANCE.getRandomBytes(INNER_PADDING_BYTES);
        textAndPaddingBuilder.setPadding(ByteString.copyFrom(padding));

        Inner.InnerData innerData = innerDataBuilder.build();

        return innerData;
    }
}
