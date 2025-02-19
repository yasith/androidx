/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        instrumentedPackages = {"androidx.camera.camera2.internal"})
public class Camera2CameraInfoImplTest {
    private static final String CAMERA0_ID = "0";
    private static final int CAMERA0_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    private static final int CAMERA0_SENSOR_ORIENTATION = 90;
    @CameraSelector.LensFacing
    private static final int CAMERA0_LENS_FACING_ENUM = CameraSelector.LENS_FACING_BACK;
    private static final int CAMERA0_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_BACK;
    private static final boolean CAMERA0_FLASH_INFO_BOOLEAN = true;

    private static final String CAMERA1_ID = "1";
    private static final int CAMERA1_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3;
    private static final int CAMERA1_SENSOR_ORIENTATION = 0;
    private static final int CAMERA1_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_FRONT;
    private static final boolean CAMERA1_FLASH_INFO_BOOLEAN = false;

    private CameraCharacteristicsCompat mCameraCharacteristics0;
    private CameraCharacteristicsCompat mCameraCharacteristics1;
    private CameraManagerCompat mCameraManagerCompat;
    private ZoomControl mMockZoomControl;
    private TorchControl mMockTorchControl;
    private ExposureControl mExposureControl;
    private FocusMeteringControl mFocusMeteringControl;
    private Camera2CameraControlImpl mMockCameraControl;

    @Before
    public void setUp() throws CameraAccessExceptionCompat {
        initCameras();

        mCameraManagerCompat =
                CameraManagerCompat.from((Context) ApplicationProvider.getApplicationContext());
        mCameraCharacteristics0 = mCameraManagerCompat.getCameraCharacteristicsCompat(CAMERA0_ID);
        mCameraCharacteristics1 = mCameraManagerCompat.getCameraCharacteristicsCompat(CAMERA1_ID);

        mMockZoomControl = mock(ZoomControl.class);
        mMockTorchControl = mock(TorchControl.class);
        mExposureControl = mock(ExposureControl.class);
        mMockCameraControl = mock(Camera2CameraControlImpl.class);
        mFocusMeteringControl = mock(FocusMeteringControl.class);

        when(mMockCameraControl.getZoomControl()).thenReturn(mMockZoomControl);
        when(mMockCameraControl.getTorchControl()).thenReturn(mMockTorchControl);
        when(mMockCameraControl.getExposureControl()).thenReturn(mExposureControl);
        when(mMockCameraControl.getFocusMeteringControl()).thenReturn(mFocusMeteringControl);
    }

    @Test
    public void canCreateCameraInfo() throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);

        assertThat(cameraInfoInternal).isNotNull();
    }

    @Test
    public void cameraInfo_canReturnSensorOrientation() throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        assertThat(cameraInfoInternal.getSensorRotationDegrees()).isEqualTo(
                CAMERA0_SENSOR_ORIENTATION);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forBackCamera()
            throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);

        // Note: these numbers depend on the camera being a back-facing camera.
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA0_SENSOR_ORIENTATION);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 90 + 360) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 180 + 360) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA0_SENSOR_ORIENTATION - 270 + 360) % 360);
    }

    @Test
    public void cameraInfo_canCalculateCorrectRelativeRotation_forFrontCamera()
            throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA1_ID, mCameraManagerCompat);

        // Note: these numbers depend on the camera being a front-facing camera.
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_0))
                .isEqualTo(CAMERA1_SENSOR_ORIENTATION);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_90))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 90) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_180))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 180) % 360);
        assertThat(cameraInfoInternal.getSensorRotationDegrees(Surface.ROTATION_270))
                .isEqualTo((CAMERA1_SENSOR_ORIENTATION + 270) % 360);
    }

    @Test
    public void cameraInfo_canReturnLensFacing() throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        assertThat(cameraInfoInternal.getLensFacing()).isEqualTo(CAMERA0_LENS_FACING_ENUM);
    }

    @Test
    public void cameraInfo_canReturnHasFlashUnit_forBackCamera()
            throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        assertThat(cameraInfoInternal.hasFlashUnit()).isEqualTo(CAMERA0_FLASH_INFO_BOOLEAN);
    }

    @Test
    public void cameraInfo_canReturnHasFlashUnit_forFrontCamera()
            throws CameraAccessExceptionCompat {
        CameraInfoInternal cameraInfoInternal =
                new Camera2CameraInfoImpl(CAMERA1_ID, mCameraManagerCompat);
        assertThat(cameraInfoInternal.hasFlashUnit()).isEqualTo(CAMERA1_FLASH_INFO_BOOLEAN);
    }

    @Test
    public void cameraInfoWithoutCameraControl_canReturnDefaultTorchState()
            throws CameraAccessExceptionCompat {
        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        assertThat(camera2CameraInfoImpl.getTorchState().getValue())
                .isEqualTo(TorchControl.DEFAULT_TORCH_STATE);
    }

    @Test
    public void cameraInfoWithCameraControl_canReturnTorchState()
            throws CameraAccessExceptionCompat {
        when(mMockTorchControl.getTorchState()).thenReturn(new MutableLiveData<>(TorchState.ON));
        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        camera2CameraInfoImpl.linkWithCameraControl(mMockCameraControl);
        assertThat(camera2CameraInfoImpl.getTorchState().getValue()).isEqualTo(TorchState.ON);
    }

    @Test
    public void torchStateLiveData_SameInstanceBeforeAndAfterCameraControlLink()
            throws CameraAccessExceptionCompat {
        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);

        // Calls getTorchState() to trigger RedirectableLiveData
        LiveData<Integer> torchStateLiveData = camera2CameraInfoImpl.getTorchState();

        when(mMockTorchControl.getTorchState()).thenReturn(new MutableLiveData<>(TorchState.ON));
        camera2CameraInfoImpl.linkWithCameraControl(mMockCameraControl);

        // TorchState LiveData instances are the same before and after the linkWithCameraControl.
        assertThat(camera2CameraInfoImpl.getTorchState()).isSameInstanceAs(torchStateLiveData);
        assertThat(camera2CameraInfoImpl.getTorchState().getValue()).isEqualTo(TorchState.ON);
    }

    // zoom related tests just ensure it uses ZoomControl to get the value
    // Full tests are performed at ZoomControlDeviceTest / ZoomControlTest.
    @Test
    public void cameraInfoWithCameraControl_getZoom_valueIsCorrect()
            throws CameraAccessExceptionCompat {
        ZoomState zoomState = ImmutableZoomState.create(3.0f, 8.0f, 1.0f, 0.2f);
        when(mMockZoomControl.getZoomState()).thenReturn(new MutableLiveData<>(zoomState));

        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        camera2CameraInfoImpl.linkWithCameraControl(mMockCameraControl);

        assertThat(camera2CameraInfoImpl.getZoomState().getValue()).isEqualTo(zoomState);
    }

    @Test
    public void cameraInfoWithoutCameraControl_getDetaultZoomState()
            throws CameraAccessExceptionCompat {
        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        assertThat(camera2CameraInfoImpl.getZoomState().getValue())
                .isEqualTo(ZoomControl.getDefaultZoomState(mCameraCharacteristics0));
    }

    @Test
    public void zoomStateLiveData_SameInstanceBeforeAndAfterCameraControlLink()
            throws CameraAccessExceptionCompat {
        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);

        // Calls getZoomState() to trigger RedirectableLiveData
        LiveData<ZoomState> zoomStateLiveData = camera2CameraInfoImpl.getZoomState();

        ZoomState zoomState = ImmutableZoomState.create(3.0f, 8.0f, 1.0f, 0.2f);
        when(mMockZoomControl.getZoomState()).thenReturn(new MutableLiveData<>(zoomState));
        camera2CameraInfoImpl.linkWithCameraControl(mMockCameraControl);

        // TorchState LiveData instances are the same before and after the linkWithCameraControl.
        assertThat(camera2CameraInfoImpl.getZoomState()).isSameInstanceAs(zoomStateLiveData);
        assertThat(camera2CameraInfoImpl.getZoomState().getValue()).isEqualTo(zoomState);
    }

    @Test
    public void cameraInfoWithCameraControl_canReturnExposureState()
            throws CameraAccessExceptionCompat {
        ExposureState exposureState = new ExposureStateImpl(mCameraCharacteristics0, 2);
        when(mExposureControl.getExposureState()).thenReturn(exposureState);

        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        camera2CameraInfoImpl.linkWithCameraControl(mMockCameraControl);

        assertThat(camera2CameraInfoImpl.getExposureState()).isEqualTo(exposureState);
    }

    @Test
    public void cameraInfoWithoutCameraControl_canReturnDefaultExposureState()
            throws CameraAccessExceptionCompat {
        Camera2CameraInfoImpl camera2CameraInfoImpl =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);

        ExposureState defaultState =
                ExposureControl.getDefaultExposureState(mCameraCharacteristics0);

        assertThat(camera2CameraInfoImpl.getExposureState().getExposureCompensationIndex())
                .isEqualTo(defaultState.getExposureCompensationIndex());
        assertThat(camera2CameraInfoImpl.getExposureState().getExposureCompensationRange())
                .isEqualTo(defaultState.getExposureCompensationRange());
        assertThat(camera2CameraInfoImpl.getExposureState().getExposureCompensationStep())
                .isEqualTo(defaultState.getExposureCompensationStep());
        assertThat(camera2CameraInfoImpl.getExposureState().isExposureCompensationSupported())
                .isEqualTo(defaultState.isExposureCompensationSupported());
    }

    @Test
    public void cameraInfo_getImplementationType_legacy() throws CameraAccessExceptionCompat {
        final CameraInfoInternal cameraInfo =
                new Camera2CameraInfoImpl(CAMERA0_ID, mCameraManagerCompat);
        assertThat(cameraInfo.getImplementationType()).isEqualTo(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);
    }

    @Test
    public void cameraInfo_getImplementationType_noneLegacy() throws CameraAccessExceptionCompat {
        final CameraInfoInternal cameraInfo = new Camera2CameraInfoImpl(
                CAMERA1_ID, mCameraManagerCompat);
        assertThat(cameraInfo.getImplementationType()).isEqualTo(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);
    }

    @Test
    public void addSessionCameraCaptureCallback_isCalledToCameraControl()
            throws CameraAccessExceptionCompat {
        final Camera2CameraInfoImpl cameraInfo = new Camera2CameraInfoImpl(
                CAMERA1_ID, mCameraManagerCompat);
        cameraInfo.linkWithCameraControl(mMockCameraControl);

        Executor executor = mock(Executor.class);
        CameraCaptureCallback callback = mock(CameraCaptureCallback.class);
        cameraInfo.addSessionCaptureCallback(executor, callback);

        verify(mMockCameraControl).addSessionCameraCaptureCallback(executor, callback);
    }

    @Test
    public void removeSessionCameraCaptureCallback_isCalledToCameraControl()
            throws CameraAccessExceptionCompat {
        final Camera2CameraInfoImpl cameraInfo = new Camera2CameraInfoImpl(
                CAMERA1_ID, mCameraManagerCompat);
        cameraInfo.linkWithCameraControl(mMockCameraControl);

        CameraCaptureCallback callback = mock(CameraCaptureCallback.class);
        cameraInfo.removeSessionCaptureCallback(callback);

        verify(mMockCameraControl).removeSessionCameraCaptureCallback(callback);
    }

    @Test
    public void addSessionCameraCaptureCallbackWithoutCameraControl_attachedToCameraControlLater()
            throws CameraAccessExceptionCompat {
        final Camera2CameraInfoImpl cameraInfo = new Camera2CameraInfoImpl(
                CAMERA1_ID, mCameraManagerCompat);
        Executor executor = mock(Executor.class);
        CameraCaptureCallback callback = mock(CameraCaptureCallback.class);
        cameraInfo.addSessionCaptureCallback(executor, callback);

        cameraInfo.linkWithCameraControl(mMockCameraControl);

        verify(mMockCameraControl).addSessionCameraCaptureCallback(executor, callback);
    }

    @Test
    public void removeSessionCameraCaptureCallbackWithoutCameraControl_callbackIsRemoved()
            throws CameraAccessExceptionCompat {
        final Camera2CameraInfoImpl cameraInfo = new Camera2CameraInfoImpl(
                CAMERA1_ID, mCameraManagerCompat);
        // Add two callbacks
        Executor executor1 = mock(Executor.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        Executor executor2 = mock(Executor.class);
        CameraCaptureCallback callback2 = mock(CameraCaptureCallback.class);
        cameraInfo.addSessionCaptureCallback(executor1, callback1);
        cameraInfo.addSessionCaptureCallback(executor2, callback2);

        // Remove first callback.
        cameraInfo.removeSessionCaptureCallback(callback1);

        // Only second callback will be added to camera control.
        cameraInfo.linkWithCameraControl(mMockCameraControl);
        verify(mMockCameraControl, never()).addSessionCameraCaptureCallback(executor1, callback1);
        verify(mMockCameraControl).addSessionCameraCaptureCallback(executor2, callback2);
    }

    @Test
    public void cameraInfoWithCameraControl_canReturnIsFocusMeteringSupported()
            throws CameraAccessExceptionCompat {
        final Camera2CameraInfoImpl cameraInfo = new Camera2CameraInfoImpl(
                CAMERA0_ID, mCameraManagerCompat);

        cameraInfo.linkWithCameraControl(mMockCameraControl);

        when(mFocusMeteringControl.isFocusMeteringSupported(any(FocusMeteringAction.class)))
                .thenReturn(true);

        SurfaceOrientedMeteringPointFactory factory =
                new SurfaceOrientedMeteringPointFactory(1.0f, 1.0f);
        FocusMeteringAction action = new FocusMeteringAction.Builder(
                factory.createPoint(0.5f, 0.5f)).build();
        assertThat(cameraInfo.isFocusMeteringSupported(action)).isTrue();
    }

    @Config(minSdk = 28)
    @RequiresApi(28)
    @Test
    public void canReturnCameraCharacteristicsMapWithPhysicalCameras()
            throws CameraAccessExceptionCompat {
        CameraCharacteristics characteristics0 = mock(CameraCharacteristics.class);
        CameraCharacteristics characteristicsPhysical2 = mock(CameraCharacteristics.class);
        CameraCharacteristics characteristicsPhysical3 = mock(CameraCharacteristics.class);
        when(characteristics0.getPhysicalCameraIds())
                .thenReturn(new HashSet<>(Arrays.asList("0", "2", "3")));
        CameraManagerCompat cameraManagerCompat = initCameraManagerWithPhysicalIds(
                Arrays.asList(
                        new Pair<>("0", characteristics0),
                        new Pair<>("2", characteristicsPhysical2),
                        new Pair<>("3", characteristicsPhysical3)));
        Camera2CameraInfoImpl impl = new Camera2CameraInfoImpl("0", cameraManagerCompat);

        Map<String, CameraCharacteristics> map = impl.getCameraCharacteristicsMap();
        assertThat(map.size()).isEqualTo(3);
        assertThat(map.get("0")).isSameInstanceAs(characteristics0);
        assertThat(map.get("2")).isSameInstanceAs(characteristicsPhysical2);
        assertThat(map.get("3")).isSameInstanceAs(characteristicsPhysical3);
    }

    @Config(maxSdk = 27)
    @Test
    public void canReturnCameraCharacteristicsMapWithMainCamera()
            throws CameraAccessExceptionCompat {
        Camera2CameraInfoImpl impl = new Camera2CameraInfoImpl("0", mCameraManagerCompat);
        Map<String, CameraCharacteristics> map = impl.getCameraCharacteristicsMap();
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get("0"))
                .isSameInstanceAs(mCameraCharacteristics0.toCameraCharacteristics());
    }

    private CameraManagerCompat initCameraManagerWithPhysicalIds(
            List<Pair<String, CameraCharacteristics>> cameraIdsAndCharacteristicsList) {
        FakeCameraManagerImpl cameraManagerImpl = new FakeCameraManagerImpl();
        for (Pair<String, CameraCharacteristics> pair : cameraIdsAndCharacteristicsList) {
            String cameraId = pair.first;
            CameraCharacteristics cameraCharacteristics = pair.second;
            cameraManagerImpl.addCamera(cameraId, cameraCharacteristics);
        }
        return CameraManagerCompat.from(cameraManagerImpl);
    }



    private void initCameras() {
        // **** Camera 0 characteristics ****//
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);

        shadowCharacteristics0.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CAMERA0_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics0.set(CameraCharacteristics.LENS_FACING, CAMERA0_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics0.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA0_SENSOR_ORIENTATION);

        // Mock the flash unit availability
        shadowCharacteristics0.set(
                CameraCharacteristics.FLASH_INFO_AVAILABLE, CAMERA0_FLASH_INFO_BOOLEAN);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        // **** Camera 1 characteristics ****//
        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);

        shadowCharacteristics1.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CAMERA1_SUPPORTED_HARDWARE_LEVEL);

        // Add a lens facing to the camera
        shadowCharacteristics1.set(CameraCharacteristics.LENS_FACING, CAMERA1_LENS_FACING_INT);

        // Mock the sensor orientation
        shadowCharacteristics1.set(
                CameraCharacteristics.SENSOR_ORIENTATION, CAMERA1_SENSOR_ORIENTATION);

        // Mock the flash unit availability
        shadowCharacteristics1.set(
                CameraCharacteristics.FLASH_INFO_AVAILABLE, CAMERA1_FLASH_INFO_BOOLEAN);

        // Add the camera to the camera service
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);
    }

    private static class FakeCameraManagerImpl
            implements CameraManagerCompat.CameraManagerCompatImpl {
        private final HashMap<String, CameraCharacteristics> mCameraIdCharacteristics =
                new HashMap<>();

        public void addCamera(@NonNull String cameraId,
                @NonNull CameraCharacteristics cameraCharacteristics) {
            mCameraIdCharacteristics.put(cameraId, cameraCharacteristics);
        }
        @NonNull
        @Override
        public String[] getCameraIdList() throws CameraAccessExceptionCompat {
            return mCameraIdCharacteristics.keySet().toArray(new String[0]);
        }

        @Override
        public void registerAvailabilityCallback(@NonNull Executor executor,
                @NonNull CameraManager.AvailabilityCallback callback) {
        }

        @Override
        public void unregisterAvailabilityCallback(
                @NonNull CameraManager.AvailabilityCallback callback) {
        }

        @NonNull
        @Override
        public CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId)
                throws CameraAccessExceptionCompat {
            return mCameraIdCharacteristics.get(cameraId);
        }

        @Override
        public void openCamera(@NonNull String cameraId, @NonNull Executor executor,
                @NonNull CameraDevice.StateCallback callback) throws CameraAccessExceptionCompat {

        }

        @NonNull
        @Override
        public CameraManager getCameraManager() {
            return null;
        }
    }
}
