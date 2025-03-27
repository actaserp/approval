package mes.app.MobileUsr.AppInfo;


import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class PagingService {

    private int BAR_LENGTH = 5;

    public List<Integer> pagingList(int currentPage, int totalPage){
        int startNum = Math.max( currentPage - (BAR_LENGTH/2),0);
        int endNum = Math.min(startNum + BAR_LENGTH, totalPage);

        // 시작 페이지를 보정 (마지막 페이지 범위 초과 방지)
        startNum = Math.max(endNum - BAR_LENGTH, 0);

        return IntStream.range(startNum, endNum).boxed().toList();
    }

    public int getBAR_LENGTH(){
        return BAR_LENGTH;
    }
}
