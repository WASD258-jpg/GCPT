package cn.gcpt.app;

import java.util.*;
import java.util.regex.*;

/** No Android dependencies: deterministic, fail-closed parsing, also tested on desktop JVM. */
public final class CourseParser {
    public static final class Course {
        public String name, room, teacher, weeksText, raw, group, hours;
        public int day, start, end;
        public SortedSet<Integer> weeks = new TreeSet<Integer>();
        public boolean adjusted;
    }
    public static final class Result {
        public final List<Course> courses = new ArrayList<Course>();
        public final List<String> pending = new ArrayList<String>();
    }
    private static final Pattern TIME = Pattern.compile("[（(]\\s*(\\d{1,2})(?:\\s*[-－~～至]\\s*(\\d{1,2}))?\\s*节\\s*[）)]([^\\r\\n]*)");
    public static SortedSet<Integer> parseWeeks(String value) {
        String v=value.replaceAll("\\s", "").replace("第", "").replace("周", "")
            .replace("（单）", "单").replace("(单)", "单").replace("（双）", "双").replace("(双)", "双")
            .replace('，', ',').replace('、', ',').replace('；', ',').replace(';', ',');
        if(v.isEmpty())throw new IllegalArgumentException("周次为空");
        SortedSet<Integer> result=new TreeSet<Integer>();
        for(String part:v.split(",",-1)) {
            Matcher m=Pattern.compile("^(\\d{1,2})(?:[-－~～至](\\d{1,2}))?([单双])?$").matcher(part);
            if(!m.matches())throw new IllegalArgumentException("周次格式无法确认");
            int a=Integer.parseInt(m.group(1)),b=m.group(2)==null?a:Integer.parseInt(m.group(2));
            if(a<1||b>30||a>b)throw new IllegalArgumentException("周次超出范围");
            for(int n=a;n<=b;n++)if(m.group(3)==null||("单".equals(m.group(3))&&n%2==1)||("双".equals(m.group(3))&&n%2==0))result.add(n);
        }
        if(result.isEmpty())throw new IllegalArgumentException("没有有效周次");
        return result;
    }
    public static Result parseCell(int day,String raw,String status) {
        Result result=new Result();
        if(day<1||day>7||"pending".equals(status)){result.pending.add(("pending".equals(status)?"待筛选课程，不排入课表：":"星期无法确认：")+raw);return result;}
        String text=raw.replace('\u00a0',' ').replace("\r\n","\n").trim();
        List<int[]> bounds=new ArrayList<int[]>();List<String[]> values=new ArrayList<String[]>();
        Matcher m=TIME.matcher(text);
        while(m.find()){bounds.add(new int[]{m.start(),m.end()});values.add(new String[]{m.group(1),m.group(2),m.group(3)});}
        if(bounds.isEmpty()){result.pending.add("节次无法确认："+raw);return result;}
        int[] nameStarts=new int[bounds.size()];String[] names=new String[bounds.size()];
        for(int i=0;i<bounds.size();i++){
            int end=bounds.get(i)[0];while(end>0&&Character.isWhitespace(text.charAt(end-1)))end--;
            int begin=text.lastIndexOf('\n',Math.max(0,end-1))+1;
            names[i]=text.substring(begin,end).trim();nameStarts[i]=begin;
        }
        for(int i=0;i<bounds.size();i++) {
            int blockEnd=i+1<bounds.size()?nameStarts[i+1]:text.length();
            String block=text.substring(nameStarts[i],Math.max(bounds.get(i)[1],blockEnd));
            try{
                Course c=new Course();c.day=day;c.name=names[i];c.raw=block;
                c.start=Integer.parseInt(values.get(i)[0]);c.end=values.get(i)[1]==null?c.start:Integer.parseInt(values.get(i)[1]);
                if(c.name.isEmpty()||c.start<1||c.end>16||c.start>c.end)throw new IllegalArgumentException("课程名称或节次无法确认");
                c.weeksText=values.get(i)[2].trim();c.weeks=parseWeeks(c.weeksText);
                String tail=text.substring(bounds.get(i)[1],Math.max(bounds.get(i)[1],blockEnd)).trim();
                String[] lines=tail.isEmpty()?new String[0]:tail.split("\\n+");
                c.room=lines.length>0?lines[0].trim().replaceFirst("^上课地点[：:]\\s*",""):"地点待确认";
                c.teacher=lines.length>1?lines[1].trim().replaceFirst("^教师[：:]\\s*",""):"";
                c.group=lines.length>2?lines[2].trim().replaceFirst("^班级[：:]\\s*",""):"";
                c.hours=lines.length>3?lines[3].trim().replaceFirst("^学时[：:]\\s*",""):"";
                c.adjusted=c.name.contains("【调】")||c.name.contains("[调]");
                result.courses.add(c);
            }catch(IllegalArgumentException ex){result.pending.add(ex.getMessage()+"：\n"+block);}
        }
        return result;
    }
    public static List<String> pendingItems(String text){
        List<String> items=new ArrayList<String>();
        if(text.startsWith("其它课程")||text.startsWith("其他课程")){
            String body=text.replaceFirst("^[其][它他]课程[：:]?\\s*","");
            for(String part:body.split("[;；]"))if(!part.trim().isEmpty())items.add("未提供明确星期/节次，不排入周课表：\n"+part.trim());
        }else if(!text.trim().isEmpty())items.add(text);
        return items;
    }
    public static boolean overlaps(Course a,Course b,int week){return a.day==b.day&&a.weeks.contains(week)&&b.weeks.contains(week)&&a.start<=b.end&&b.start<=a.end;}
}
