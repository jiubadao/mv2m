package it.cosenonjaviste.mv2m;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

public class ViewModelManager {
    public static final String MODEL = "model";
    public static final String RESULT_DATA = "RESULT_DATA";

    public static <VM extends ViewModel<?>> VM getOrCreate(AppCompatActivity activity, Bundle state, String fragmentTag, ViewModelManager.Factory<VM> factory) {
        VM viewModel = ViewModelRetainedFragment.getOrCreate(activity, state, fragmentTag, factory);
        viewModel.attachActivity(activity);
        return viewModel;
    }

    public static void resume(Object view, ViewModel<?> viewModel) {
        viewModel.resume();
    }

    public static void pause(ViewModel<?> viewModel) {
        viewModel.pause();
    }

    public static void destroy(ViewModel<?> viewModel) {
        viewModel.detachView();
    }

    public static void saveState(final Bundle outState, ViewModel<?> viewModel) {
        outState.putParcelable(MODEL, viewModel.getModel());
    }

    @NonNull public static <VM extends ViewModel<?>> VM init(VM viewModel, Bundle state, Bundle args, Factory<VM> factory) {
        if (viewModel == null) {
            viewModel = factory.create();
        }
        Parcelable model = null;
        if (state != null) {
            model = state.getParcelable(MODEL);
        }
        if (model == null && args != null) {
            model = args.getParcelable(MODEL);
        }
        ((ViewModel) viewModel).initModel(model);
        return viewModel;
    }

    public static <VM extends ViewModel<?>> void onBackPressed(Activity activity, VM viewModel) {
        ActivityResult result = viewModel.onBackPressed();
        if (result != null) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_DATA, result.getData());
            activity.setResult(result.isResultOk() ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        }
    }

    public static <VM extends ViewModel<?>> void onActivityResult(VM viewModel, int requestCode, int resultCode, Intent data) {
        viewModel.onResult(requestCode, new ActivityResult(resultCode == Activity.RESULT_OK, data.getParcelableExtra(RESULT_DATA)));
    }

    public interface Factory<VM extends ViewModel<?>> {
        VM create();
    }
}
