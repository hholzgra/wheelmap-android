package org.wheelmap.android.fragment;

import java.util.HashMap;

import org.wheelmap.android.online.R;

import wheelmap.org.WheelchairState;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.actionbarsherlock.app.SherlockFragment;

public class WheelchairStateFragment extends SherlockFragment implements
		OnClickListener {
	public static final String TAG = WheelchairStateFragment.class
			.getSimpleName();

	public static final String EXTRA_WHEELCHAIR_STATE = "org.wheelmap.android.EXTRA_WHEELCHAIR_STATE";
	private HashMap<WheelchairState, RadioButton> mRadioButtonsMap = new HashMap<WheelchairState, RadioButton>();
	private OnWheelchairState mListener;

	public interface OnWheelchairState {
		public void onWheelchairStateSelect(WheelchairState state);
	}

	public static WheelchairStateFragment newInstance(WheelchairState state) {
		Bundle b = new Bundle();
		b.putInt(EXTRA_WHEELCHAIR_STATE, state.getId());
		WheelchairStateFragment f = new WheelchairStateFragment();
		f.setArguments(b);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof OnWheelchairState)
			mListener = (OnWheelchairState) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_wheelchair_state,
				container, false);

		mRadioButtonsMap.put(WheelchairState.YES,
				(RadioButton) v.findViewById(R.id.radio_enabled));
		mRadioButtonsMap.put(WheelchairState.LIMITED,
				(RadioButton) v.findViewById(R.id.radio_limited));
		mRadioButtonsMap.put(WheelchairState.NO,
				(RadioButton) v.findViewById(R.id.radio_disabled));
		mRadioButtonsMap.put(WheelchairState.UNKNOWN,
				(RadioButton) v.findViewById(R.id.radio_unknown));

		for (WheelchairState state : mRadioButtonsMap.keySet()) {
			mRadioButtonsMap.get(state).setOnClickListener(this);
		}

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int newStateInt = getArguments().getInt(EXTRA_WHEELCHAIR_STATE, -1);
		WheelchairState newState = WheelchairState.valueOf(newStateInt);
		setWheelchairState(newState);

	}

	private void DeselectAllRadioButtons() {
		for (WheelchairState state : mRadioButtonsMap.keySet()) {
			mRadioButtonsMap.get(state).setChecked(false);
		}
	}

	private void setWheelchairState(WheelchairState newState) {
		DeselectAllRadioButtons();
		mRadioButtonsMap.get(newState).setChecked(true);
	}

	private WheelchairState getWheelchairState() {
		for (WheelchairState state : mRadioButtonsMap.keySet()) {
			if (mRadioButtonsMap.get(state).isChecked())
				return state;
		}
		return WheelchairState.UNKNOWN;
	}

	@Override
	public void onClick(View v) {
		DeselectAllRadioButtons();
		final RadioButton a = (RadioButton) v;
		a.setChecked(true);

		if (mListener != null)
			mListener.onWheelchairStateSelect(getWheelchairState());
	}

}