package jp.aiaida.panelpreset;

import android.database.Cursor;

public class SliderProperty {
//Property
	private Long id = null;					// #0
	private String name = null;				// #1
	private String caption = null;			// #2
	private Long range_min = null;			// #3
	private Long range_max = null;			// #4
	private Long normalize_factor = null;	// #5
	private Long scale_min = null;			// #6
	private Long scale_max = null;			// #7
	private Double scale_resolution = null;	// #8
	private String label_min = null;		// #9
	private String label_max = null;		// #10
	private String label_unit = null;		// #11

	public String getName(){
		return name;
	}

	public String getCaption(){
		return caption;
	}

	public String getLabelMin(){
		return label_min;
	}

	public String getLabelMax(){
		return label_max;
	}

	public String getLabelUnit(){
		return label_unit;
	}

	public Long getScaleMin(){
		return scale_min;
	}

	public Long getScaleMax(){
		return scale_max;
	}

	public Double getScaleResolution(){
		return scale_resolution;
	}

	public Long getNormaraizeFactor(){
		return normalize_factor;
	}

	public static SliderProperty extractSliderProperty(Cursor cursor){
		SliderProperty property = new SliderProperty();

		property.id = cursor.getLong(0);					// #0
		property.name = cursor.getString(1);				// #1
		property.caption = cursor.getString(2);				// #2
		property.range_min = cursor.getLong(3);				// #3
		property.range_min = cursor.getLong(4);				// #4
		property.normalize_factor = cursor.getLong(5);		// #5
		property.scale_min = cursor.getLong(6);				// #6
		property.scale_max = cursor.getLong(7);				// #7
		property.scale_resolution = cursor.getDouble(8);	// #8
		property.label_min = cursor.getString(9);			// #9
		property.label_max = cursor.getString(10);			// #10
		property.label_unit = cursor.getString(11);			// #11
		return property;

	}
}
