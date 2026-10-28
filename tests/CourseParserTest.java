import cn.gcpt.app.CourseParser;
import java.util.*;
public class CourseParserTest {
    static int passed=0;
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);passed++;}
    static void reject(String value){try{CourseParser.parseWeeks(value);throw new AssertionError("Accepted invalid: "+value);}catch(IllegalArgumentException ok){passed++;}}
    public static void main(String[] args){
        check(CourseParser.parseWeeks("1-5、7-11单、12-16双").toString().equals("[1, 2, 3, 4, 5, 7, 9, 11, 12, 14, 16]"),"mixed weeks");
        check(CourseParser.parseWeeks("第13周").contains(13),"single week");
        check(CourseParser.parseWeeks("2-5周,7-10周").size()==8,"multiple intervals");
        check(CourseParser.parseWeeks("1-16周(双)").size()==8,"even weeks");
        check(CourseParser.parseWeeks("2、5、8").size()==3,"discrete weeks");
        for(String s:new String[]{"","0-2","4-1","1-31","1-2,","每周","1-2未知","2单"})reject(s);
        String a="【调】测试课程\n(1-4节)2-5周,7-10周\n测试楼A101\n教师甲\n测试班\n64";
        CourseParser.Result r=CourseParser.parseCell(2,a,"page");
        check(r.courses.size()==1&&r.pending.isEmpty(),"valid block");
        CourseParser.Course c=r.courses.get(0);
        check(c.day==2&&c.start==1&&c.end==4&&c.adjusted,"day and periods");
        check(c.room.equals("测试楼A101")&&c.teacher.equals("教师甲"),"room teacher");
        check(c.group.equals("测试班")&&c.hours.equals("64"),"group hours");
        check(c.weeks.contains(7)&&!c.weeks.contains(6),"week filtering");
        String b="第二课程\n（5-6节）第13周\n测试楼B202\n教师乙";
        check(CourseParser.parseCell(2,a+"\n"+b,"page").courses.size()==2,"multiple courses same cell");
        check(CourseParser.parseCell(0,a,"page").courses.isEmpty(),"unknown day must not schedule");
        check(CourseParser.parseCell(2,a,"pending").courses.isEmpty(),"unselected must not schedule");
        check(CourseParser.parseCell(2,a.replace("2-5周,7-10周","待定"),"page").pending.size()==1,"unknown weeks pending");
        check(CourseParser.parseCell(2,a.replace("1-4节","4-1节"),"page").courses.isEmpty(),"reversed periods");
        CourseParser.Course d=CourseParser.parseCell(2,a.replace("1-4节","3-5节"),"page").courses.get(0);
        check(CourseParser.overlaps(c,d,2)&&!CourseParser.overlaps(c,d,6),"overlap only active week");
        check(!CourseParser.overlaps(c,CourseParser.parseCell(3,a,"page").courses.get(0),2),"different weekdays");
        check(CourseParser.pendingItems("其它课程：体育甲/2-8周/无;体育乙/5-8周/无;").size()==2,"other courses separated");
        check(CourseParser.pendingItems("星期无法确认：测试").size()==1,"other errors preserved");
        System.out.println("PASS: "+passed+" assertions");
    }
}
