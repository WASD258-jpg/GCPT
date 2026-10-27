package cn.gct.demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The three hour ledgers returned by the school student-affairs app. */
public final class HoursData {
    public static final class Item {
        public final String name;
        public final double hours;

        Item(String name, double hours) {
            this.name = name;
            this.hours = hours;
        }
    }

    public final List<Item> psy = new ArrayList<Item>();
    public final List<Item> labor = new ArrayList<Item>();
    public final List<Item> ie = new ArrayList<Item>();

    public static HoursData from(JSONObject obj) throws JSONException {
        HoursData data = new HoursData();
        read(obj.optJSONArray("psy"), data.psy);
        read(obj.optJSONArray("labor"), data.labor);
        read(obj.optJSONArray("ie"), data.ie);
        return data;
    }

    private static void read(JSONArray array, List<Item> target) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String name = item.optString("activityName", "").trim();
            if (name.isEmpty()) name = item.optString("name", "未命名活动");
            target.add(new Item(name, item.optDouble("classHour", 0)));
        }
    }

    public static double total(List<Item> list) {
        double sum = 0;
        for (Item item : list) sum += item.hours;
        return sum;
    }

    public double psyTotal() {
        return total(psy);
    }

    public double laborTotal() {
        return total(labor);
    }

    public double ieTotal() {
        return total(ie);
    }

    public double total() {
        return psyTotal() + laborTotal() + ieTotal();
    }
}
