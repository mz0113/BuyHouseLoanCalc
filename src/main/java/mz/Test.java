package main.java.mz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * 等额本金方式还款计算器
 */
public class Test {

    static double rateYear = 0.0519;
    static double totalAmount = 51 * 10000;
    static double totalPeriod = 360;

    public static void main(String[] args) throws Exception {

        /**
         * 商贷4次提前还款案例封装
         */
        Callable<List<String>> businesslending = new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                List<AdvanceCase.AdvanceElement> elementList = new ArrayList<>();

                //现在还 7.5万
                elementList.add(new AdvanceCase.AdvanceElement(10, 7.5 * 10000));

                //明年5月再还 20W
                elementList.add(new AdvanceCase.AdvanceElement(23, 20 * 10000));

                //后年5月再还 15W
                elementList.add(new AdvanceCase.AdvanceElement(23 + 12, 15 * 10000));

                //再过1年再还 7W
                elementList.add(new AdvanceCase.AdvanceElement(23 + 12 + 12, 7 * 10000));

                final AdvanceCase aCase = new AdvanceCase(elementList,rateYear,totalAmount,totalPeriod);
                final List<String> list = aCase.test();
                return list;
            }
        };


        /**
         * 公积金贷款案例封装
         */
        Callable<List<String>> housingProvidentFundLoan = new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                List<AdvanceCase.AdvanceElement> elementList = new ArrayList<>();
                //明年5月还9.5W 4600*12 = 5.5W，2500*12 = 3W,剩余2.5W+7W = 9.5W
                elementList.add(new AdvanceCase.AdvanceElement(23, 9.5 * 10000));

                final AdvanceCase aCase = new AdvanceCase(elementList, 0.0325, 30 * 10000, totalPeriod);
                final List<String> list = aCase.test();
                return list;
            }
        };


        System.out.println(businesslending.call());
    }

    /**
     * 还款案例
     */
    public static class AdvanceCase{

        /**
         * 等额本金计算引擎核心
         */
        CalcWrapper calcWrapper;
        /**
         * 提前还款
         */
        List<AdvanceElement> payBackPeriodLst;

        public static class AdvanceElement{
            /**
             * 在第i期还
             */
            int i;
            /**
             * 提前还款金额
             */
            double amount;

            public AdvanceElement(int i, double amount) {
                this.i = i;
                this.amount = amount;
            }
        }

        /**
         * 构造案例
         * @param payBackPeriodLst
         * @param rateYear 年利率 5.09%传0.059
         * @param totalAmount 贷款总金额
         * @param totalPeriod 总期限数量 30年 = 360期
         */
        public AdvanceCase(List<AdvanceElement> payBackPeriodLst,double rateYear, double totalAmount, double totalPeriod) {
            this.payBackPeriodLst = payBackPeriodLst;
            calcWrapper = new CalcWrapper(rateYear,totalAmount,totalPeriod);
        }

        /**
         * 执行案例
         * @return
         */
        List<String> test(){
            final List<String> list = calcWrapper.calc(result -> {
                for (AdvanceElement element : payBackPeriodLst) {
                    if (element.i == result.i) {
                        //还款
                        calcWrapper.toPayBack(element.amount);
                    }
                }
            });
            return list;
        }
    }


    /**
     * 等额本金计算核心
     */
    public static class CalcWrapper{

        /**
         * 年利率 5.09%传0.059
         */
        private double rateYear;
        /**
         * 贷款总额
         */
        private double totalAmount;
        /**
         * 贷款期限数量
         */
        private double totalPeriod;
        /**
         * 截至目前总已还利息金额
         */
        private double alreadySumInterest;
        /**
         * 截至目前总已还本金金额
         */
        private double alreadySumPrinciple;

        /**
         * 剩余未还本金
         */
        private double rsdlTotalPrinciple;

        /**
         * 计算步骤说明
         */
        List<String> stepList = new ArrayList<>();


        public static class Result{
            public int i;
            public double alreadySumPrinciple;
            public double alreadySumInterest;

            public Result(int i, double alreadySumPrinciple, double alreadySumInterest) {
                this.i = i;
                this.alreadySumPrinciple = alreadySumPrinciple;
                this.alreadySumInterest = alreadySumInterest;
            }
        }

        public CalcWrapper(double rateYear, double totalAmount, double totalPeriod) {
            this.rateYear = rateYear;
            this.totalAmount = totalAmount;
            this.totalPeriod = totalPeriod;
            this.rsdlTotalPrinciple = totalAmount;
        }

        private CalcWrapper(){}

        public double getAlreadySumInterest() {
            return alreadySumInterest;
        }

        public double getAlreadySumPrinciple() {
            return alreadySumPrinciple;
        }

        public double getRsdlTotalPrinciple() {
            return rsdlTotalPrinciple;
        }

        /**
         * 执行提前还款动作
         */
        public void toPayBack(double money){
            if (this.rsdlTotalPrinciple > money) {
                this.rsdlTotalPrinciple -= money;
                this.alreadySumPrinciple = totalAmount - this.rsdlTotalPrinciple;
                stepList.add(String.format("提前归还本金 %.2f ,仍剩余本金 %.2f ,总归还本金%.2f \n", money, rsdlTotalPrinciple, alreadySumPrinciple));
            }else{
                this.alreadySumPrinciple = totalAmount;
                stepList.add(String.format("提前归还本金 结清 仅需归还 %.2f 传入金额 %.2f ,仍剩余本金 %.2f ,总归还本金%.2f \n", this.rsdlTotalPrinciple,money, rsdlTotalPrinciple, alreadySumPrinciple));
                this.rsdlTotalPrinciple = 0;
            }
        }

        /**
         * 开始计算
         * @param consumer 每一期扣款后执行的回调，可以执行诸如查看还款情况或者执行提前还款操作
         * @return
         */
        public List<String> calc(Consumer<Result> consumer){
            //年利率5.19%,月/期利率 = 5.19%/12
            double rateSinglePeriod = rateYear/12;

            for (int i = 1; i <= totalPeriod; i++) {

                if (rsdlTotalPrinciple <= 0) {
                    break;
                }

                //三十期
                double periodPrinciple = totalAmount/totalPeriod;
                //当期利息 = 总剩余本金*期利率
                double interest = rsdlTotalPrinciple * rateSinglePeriod;
                alreadySumInterest += interest;
                alreadySumPrinciple += periodPrinciple;
                stepList.add(String.format("第 %s 期,第 %s 年,归还利息 %.2f ,总归还利息 %.2f ,总归还本金%.2f ,总剩余本金%.2f \n", i, ((i / 12) + 1), interest, alreadySumInterest, alreadySumPrinciple, rsdlTotalPrinciple));
                //还款后总剩余本金
                rsdlTotalPrinciple -= periodPrinciple;
                consumer.accept(new Result(i,alreadySumPrinciple,alreadySumInterest));
            }

            return stepList;
        }
    }
}
