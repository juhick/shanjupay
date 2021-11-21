package com.shanjupay.transaction.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Slf4j
public class PayTestController {
    //应用Id
    private static final String APP_ID = "2021000118646755";
    //应用私钥
    private static final String APP_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDAVKBtTOcsKyQIfLECm4IcXyDM1uCg9esl0/FU705n4pWmqcU1ToukNMEyZvckORduri9x+WlwRitGV5D6rOVneb9/SMQwJrQ8Y6L58/JBftvwhuS2YXKPAIem9wishiMdOX4WOLxIDyGoB/uCSY1CX3EPcRF684oFFn53XrGAyx9fnaqgPoIfl/nc9rhFbnYBMd96xqQsde45UeetWXS4S6z737PLWAzFBim9zx8DdM5SlBCN4ehK1sHvWlHrhppN/rcCZAS97OwsFH04NF4j6ZSn9pccEJ74wCB2e2iHK5TnUh5hj3DiJZpSibi7AeXxzC3w+G3FfM92iStUGaOPAgMBAAECggEAEtE00PzRMVUXNW1dtC+az0PPZbswRw9Ibm8KEZFITomwspmxncGNkAdJT7EbRQQA8uc/5RrN4ho7aapvmNcJqtISllV8PGnNAIuPCn1/mkDbK/FGt0FZf/xBSWiqBJcrIFTbrTjn0vUICfSd+uVPWMsVuLkLSMhDHLRSs0MkgsExTcI8oxHbFJGSMLxMD36QiqRO9E9q/qywKlYYncP7ok9VVEsACpeVT1CROWCi5aVQSa9a7T8dSCR7O7EivjA1cG/0pzzvA8vR3R7pupAF99zR2pcaCkEF0dEzbYz0na4mhTX3p3yyfn8HP8hvyUHbNejavSZP5y7zEBuGCK/Y4QKBgQDuBuwgjobTMQq1xEFJoOQUFqWG7nxQjB+syXIqjWVQm8pneGyF+m/R/rizBOkqJWXvm2q0+XbruQ9ZMNDFbGbryS0i0Y+5eXHg0RQ9j99dLpYQcZ2tZYMqQjA+o7CERrfk9S/K6XxfzqV+cNxeXDroVfeIKI7JmK0AeK9OigvCEwKBgQDO2mOCgth+7MTkLyCPOt+pQRJuPhof9aEzlP5xbqmBSPFlTw4TPf9j3JXyQLqqrNyllSjarWjPPoEqdEMGDkHfXIS4J9KbaHW1PUQB9EeRVQkHB9YXz/t8lbPsu57jMDYxztIfMFraVMHV+uwh7aTLyp7Nd+GxsJxLmH9i7eloFQKBgGv9HgHDR+3m0QoYKNqjsPZgM+bcqe/NMs2scyxVDAMfOxvoaav7K+Ik41zsvHAPmYi0hRvoFgjp48m+OOloveT5V431b3RhYcXydXFcpaTb7t80/KlfZbu1Xbf9gabxgprQlkdD2NaKpVCRGZmB1IP2BKB0bhuONecB2d8TCDSbAoGAJkUXVgUZQtpRTxo8/uLYkHreixDoSnNqYJ23OYtMxwOUzbaDlyHIR1R/VXOOVZdntybSiSq+EmGxCga6g7tNiqKWV2/esivwYxizpVzphMTjx642eO9cyt7zi9/mdIft7uchzu1mUbCOsCUdXOWXsLCLEt6UHgjb0cyj/Myhe0UCgYEAqNmG06qWQ2Dq6qNRIdw/+EeuxjHs2Mt7qCyeDZuB2yPpdVm+DvObzw7oVhtuXDYmJs5DJq6bwmExcoYwUdHcpCzZmWg2Rhf4hfOWjRHpY8ZOc3l8gfI5fp0SDkTToRXlE6uu+D+Y+IvbLHe6Q82BCbF/YM83K/kHsaOhwTO2luY=";
    private static final String CHARSET = "utf-8";
    //支付宝公钥
    private static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlnoCsMQDYWjF8nmCvP3/I2qogJDUSV2OQxZdKm7Y5ETurtxo1KItTxF5T86Ca6rcOfQ5kHi6smBv3J4Xlf75wlkQTjz3+6GopcaALb+ZT86vshqIiddsxZMexYFxK26LULpegUAYJtCKElW7yGSv9tLdKuFU9y7IHskb00Fz7TpbXqFOeWTA6Ihhbcm+MoQjnewEB0rmUcgzTH58DZViorLJkzjheZRhmhv4o8bFeCN+Nvyef3gZlb9AKq8rFk115rxW/8Lfd9YpBJ5NZU+L2rNaldBk97oXgSB+kpmQgnibuxoHWo+aTj1lZZ8bKbOI8DsSapMZVwKUBOWL7j5z+QIDAQAB";
    //支付宝接口的网关地址
    private static final String SERVER_URL = "https://openapi.alipaydev.com/gateway.do";
    //签名算法类型
    private static final String SIGN_TYPE = "RSA2";

    @GetMapping(value = "/alipayTest")
    public void alipayTest(HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) throws ServletException, IOException {
        //构造SDK的客户端对象
        AlipayClient alipayClient = new DefaultAlipayClient(SERVER_URL, APP_ID, APP_PRIVATE_KEY, "json", CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//创建API对应的request
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
//        alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        alipayRequest.setBizContent("{" +
                " \"out_trade_no\":\"20150320010101002\"," +
                " \"total_amount\":\"88.88\"," +
                " \"subject\":\"Iphone6 16G\"," +
                " \"product_code\":\"QUICK_WAP_PAY\"" +
                " }");//填充业务参数
        String form="";
        try {
            //请求支付宝下单接口，发起http请求
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form);//直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }
}
