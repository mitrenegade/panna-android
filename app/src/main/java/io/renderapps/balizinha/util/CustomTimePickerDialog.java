package io.renderapps.balizinha.util;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.renderapps.balizinha.R;

public class CustomTimePickerDialog extends TimePickerDialog {

    private final static int TIME_PICKER_INTERVAL = 15;
    private TimePicker mTimePicker;
    private final OnTimeSetListener mTimeSetListener;

    public CustomTimePickerDialog(Context context, OnTimeSetListener listener,
                                  int hourOfDay, int minute, boolean is24HourView) {
        super(context, R.style.TimePickerDialogTheme, null, hourOfDay,
                minute / TIME_PICKER_INTERVAL, is24HourView);
        mTimeSetListener = listener;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            fixSpinner(context, hourOfDay, minute, is24HourView);
        }
    }

    @Override
    public void updateTime(int hourOfDay, int minuteOfHour) {
        mTimePicker.setCurrentHour(hourOfDay);
        mTimePicker.setCurrentMinute(minuteOfHour / TIME_PICKER_INTERVAL);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                if (mTimeSetListener != null) {
                    mTimeSetListener.onTimeSet(mTimePicker, mTimePicker.getCurrentHour(),
                            mTimePicker.getCurrentMinute() * TIME_PICKER_INTERVAL);
                }
                break;
            case BUTTON_NEGATIVE:
                cancel();
                break;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            Class<?> classForId = Class.forName("com.android.internal.R$id");
            Field timePickerField = classForId.getField("timePicker");

            mTimePicker = findViewById(timePickerField.getInt(null));
            Field field = classForId.getField("minute");

            NumberPicker minuteSpinner = mTimePicker
                    .findViewById(field.getInt(null));

            minuteSpinner.setMinValue(0);
            minuteSpinner.setMaxValue((60 / TIME_PICKER_INTERVAL) - 1);
            List<String> displayedValues = new ArrayList<>();

            for (int i = 0; i < 60; i += TIME_PICKER_INTERVAL) {
                displayedValues.add(String.format("%02d", i));
            }

            minuteSpinner.setDisplayedValues(displayedValues
                    .toArray(new String[displayedValues.size()]));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


/**
 * Workaround for this bug: https://code.google.com/p/android/issues/detail?id=222208
 * In Android 7.0 Nougat, spinner mode for the TimePicker in TimePickerDialog is
 * incorrectly displayed as clock, even when the theme specifies otherwise, such as:
 * */
    private void fixSpinner(Context context, int hourOfDay, int minute, boolean is24HourView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // android:timePickerMode spinner and clock began in Lollipop
            try {
                // Get the theme's android:timePickerMode
                final int MODE_SPINNER = 1;
                Class<?> styleableClass = Class.forName("com.android.internal.R$styleable");
                Field timePickerStyleableField = styleableClass.getField("TimePicker");
                int[] timePickerStyleable = (int[]) timePickerStyleableField.get(null);
                final TypedArray a = context.obtainStyledAttributes(null, timePickerStyleable, android.R.attr.timePickerStyle, 0);
                Field timePickerModeStyleableField = styleableClass.getField("TimePicker_timePickerMode");
                int timePickerModeStyleable = timePickerModeStyleableField.getInt(null);
                final int mode = a.getInt(timePickerModeStyleable, MODE_SPINNER);
                a.recycle();

                if (mode == MODE_SPINNER) {
                    Field timePickerField = findField(TimePickerDialog.class, TimePicker.class, "mTimePicker");
                    if (timePickerField == null) return;

                    TimePicker timePicker = (TimePicker) timePickerField.get(this);
                    Class<?> delegateClass = Class.forName("android.widget.TimePicker$TimePickerDelegate");
                    Field delegateField = findField(TimePicker.class, delegateClass, "mDelegate");
                    Object delegate = delegateField.get(timePicker);
                    Class<?> spinnerDelegateClass = Class.forName("android.widget.TimePickerSpinnerDelegate");
                    // In 7.0 Nougat for some reason the timePickerMode is ignored and the delegate is TimePickerClockDelegate
                    if (delegate.getClass() != spinnerDelegateClass) {
                        delegateField.set(timePicker, null); // throw out the TimePickerClockDelegate!
                        timePicker.removeAllViews(); // remove the TimePickerClockDelegate views
                        Constructor spinnerDelegateConstructor = spinnerDelegateClass.getConstructor(TimePicker.class, Context.class, AttributeSet.class, int.class, int.class);
                        spinnerDelegateConstructor.setAccessible(true);
                        // Instantiate a TimePickerSpinnerDelegate
                        delegate = spinnerDelegateConstructor.newInstance(timePicker, context, null, android.R.attr.timePickerStyle, 0);
                        delegateField.set(timePicker, delegate); // set the TimePicker.mDelegate to the spinner delegate
                        // Set up the TimePicker again, with the TimePickerSpinnerDelegate
                        timePicker.setIs24HourView(is24HourView);
                        timePicker.setCurrentHour(hourOfDay);
                        timePicker.setCurrentMinute(minute);
                        timePicker.setOnTimeChangedListener(this);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Field findField(Class objectClass, Class fieldClass, String expectedName) {
        try {
            Field field = objectClass.getDeclaredField(expectedName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {} // ignore
        // search for it if it wasn't found under the expected ivar name
        for (Field searchField : objectClass.getDeclaredFields()) {
            if (searchField.getType() == fieldClass) {
                searchField.setAccessible(true);
                return searchField;
            }
        }
        return null;
    }
}