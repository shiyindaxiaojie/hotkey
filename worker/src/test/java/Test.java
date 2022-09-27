import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.HotKeyMsg;
import com.jd.platform.hotkey.common.model.KeyCountModel;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import com.jd.platform.hotkey.common.tool.ProtostuffUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-07-28
 */
public class Test {
    public static void main(String[] args) {
        LongAdder adderCnt = new LongAdder();
        adderCnt.add(1);
        HotKeyMsg hotKeyMsg = new HotKeyMsg();
        hotKeyMsg.setAppName("cartsoa");
        HotKeyModel hotKeyModel = new HotKeyModel();
        hotKeyModel.setCount(adderCnt);
        hotKeyModel.setKey("pin_xx");
        hotKeyModel.setAppName("cartsoa");

        List<HotKeyModel> hotKeyModels = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            hotKeyModels.add(hotKeyModel);
        }
        hotKeyMsg.setHotKeyModels(hotKeyModels);

        KeyCountModel keyCountModel = new KeyCountModel();
        keyCountModel.setHotHitCount(11);
        List<KeyCountModel> keyCountModels = new ArrayList<>();
        keyCountModels.add(keyCountModel);
//        hotKeyMsg.setKeyCountModels(keyCountModels);

        byte[] serialize = ProtostuffUtils.serialize(hotKeyMsg);

        long time1 = System.currentTimeMillis();
        for (int i = 0; i < 300000; i++) {
            HotKeyMsg hhh = ProtostuffUtils.deserialize(serialize, HotKeyMsg.class);
        }
        System.out.println(System.currentTimeMillis() - time1);

        String msg = FastJsonUtils.convertObjectToJSON(hotKeyMsg);
        long time = System.currentTimeMillis();
        for (int i = 0; i < 300000; i++) {
            HotKeyMsg hhh = FastJsonUtils.toBean(msg, HotKeyMsg.class);
        }
        System.out.println(System.currentTimeMillis() - time);


    }

}
