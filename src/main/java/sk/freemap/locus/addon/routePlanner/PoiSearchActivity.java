package sk.freemap.locus.addon.routePlanner;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import menion.android.locus.addon.publiclib.utils.RequiredVersionMissingException;
import sk.freemap.locus.addon.routePlanner.pointSelection.PointSelectionActivity;
import sk.freemap.locus.addon.routePlanner.pointSelection.PointWithImage;
import android.widget.Toast;

/**
 * 
 * @author martin
 *
 */
public class PoiSearchActivity extends PointSelectionActivity {

	@Override
	protected void onPointSelected(final PointWithImage point) {
		final PointsData pd = new PointsData("callSendOnePointWithIcon");
		pd.setBitmap(getBitmap(point.getImageUrl()));
		pd.addPoint(point.getPoint());
		try {
			DisplayData.sendData(this, pd, false);
		} catch (final RequiredVersionMissingException e) {
			Toast.makeText(this, "Required version of Locus is not installed on this device.", Toast.LENGTH_LONG);
		}
	}
	
}
