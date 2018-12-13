package io.oversec.one.crypto.symsimple;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import io.oversec.one.crypto.AbstractCryptoHandler;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.gpg.GpgCryptoHandler;
import io.oversec.one.crypto.gpg.GpgEncryptionParams;
import io.oversec.one.crypto.gpg.OpenKeychainConnector;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import org.junit.Test;
import org.openintents.openpgp.IOpenPgpService2;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class GpgCryptoHandlerTest extends CryptoHandlerTestBase {


    public GpgCryptoHandlerTest() {
        OpenKeychainConnector openKeychainConnector = mock(OpenKeychainConnector.class);
        //TODO: damn, somehow need to mock this stuff (non-static)
        // when(openKeychainConnector.getVersionInternal(mContext)).thenReturn(OpenKeychainConnector.V_MIN);    // Mock implementation
        mockBoundOpenPgpService();

    }

    //TODO: see below
//    @Test
//    public void testEncryptDecrypt() throws Exception {
//
//        long keyId1 = 666L;
//        long keyId2 = 777L;
//        AbstractEncryptionParams params = new GpgEncryptionParams(new long[]{keyId1, keyId2}, null, null);
//
//
//        Inner.InnerData innerData = createInnerData(PLAIN_CONTENT);
//        Outer.Msg enc = mHandler.encrypt(innerData, params, null);
//
//
//        BaseDecryptResult decryptResult = mHandler.decrypt(enc, null, "some dummy text");
//
//        assertTrue(decryptResult.isOk());
//
//        assertEquals(decryptResult.getDecryptedDataAsInnerData(), innerData);
//        assertEquals(decryptResult.getDecryptedDataAsInnerData().getTextAndPaddingV0().getText(), PLAIN_CONTENT);
//
//    }


    @Override
    AbstractCryptoHandler createHandler() {
        return new GpgCryptoHandler(mContext);
    }


    private void mockBoundOpenPgpService() {

        LocalService.LocalBinder stubBinder = mock(LocalService.LocalBinder.class);
        when(stubBinder.getService()).thenReturn(mock(LocalService.class));
        Shadows.shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName("org.sufficientlysecure.keychain", "org.openintents.openpgp.IOpenPgpService2"), stubBinder);
    }


    public static class LocalService extends Service implements IOpenPgpService2 {
        final IBinder localBinder = new LocalBinder();

        @Override
        public IBinder onBind(Intent intent) {
            return localBinder;
        }

        @Override
        public IBinder asBinder() {
            return localBinder;
        }

        @Override
        public void onCreate() {
            super.onCreate();
        }


        @Override
        public ParcelFileDescriptor createOutputPipe(int i) throws RemoteException {
            return null;
        }

        @Override
        public Intent execute(Intent intent, ParcelFileDescriptor parcelFileDescriptor, int i) throws RemoteException {
            /**
             * TODO
             * either implement pipes and pgp enc/decryption [OVERKILL]
             *
             * or better:
             *
             * Fork Openkeychain, modify it to be a library and include it for androidTest
             * also modify it to have a method to import keys without UI
             * also maybe modify it to  not require key passwords
             * also maybe modify it to  not require any UI when encrypting / decrypting (trust all app, etc)
             */

            return null;
        }


        public class LocalBinder extends Binder {
            LocalService getService() {
                return LocalService.this;
            }
        }
    }
}