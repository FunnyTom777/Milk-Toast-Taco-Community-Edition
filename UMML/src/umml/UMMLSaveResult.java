package umml;

/**
 * The outcome of a {@link UMMLSaveSystem} operation.
 *
 * <p>Following UMML's "never throws" philosophy, every save operation returns
 * one of these instead of throwing. A failed operation carries a
 * {@link UMMLError} that explains exactly what went wrong.
 */
public class UMMLSaveResult {

    private final boolean success;
    private final UMMLError error;
    private final UMMLSaveData data;

    private UMMLSaveResult(boolean success, UMMLError error, UMMLSaveData data) {
        this.success = success;
        this.error = error;
        this.data = data;
    }

    /** Successful result carrying the loaded save data (load operations). */
    public static UMMLSaveResult success(UMMLSaveData data) {
        return new UMMLSaveResult(true, null, data);
    }

    /** Successful result with no payload (save/delete/rename). */
    public static UMMLSaveResult success() {
        return new UMMLSaveResult(true, null, null);
    }

    /** Failed result with the reason attached. Never throws. */
    public static UMMLSaveResult failure(UMMLError error) {
        return new UMMLSaveResult(false, error, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    /** The error that caused a failure, or null on success. */
    public UMMLError error() {
        return error;
    }

    /** The loaded save data (present on a successful load), or null. */
    public UMMLSaveData data() {
        return data;
    }
}
