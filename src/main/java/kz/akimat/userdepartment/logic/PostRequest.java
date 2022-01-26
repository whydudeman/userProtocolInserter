package kz.akimat.userdepartment.logic;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class PostRequest {
    public static void main(String[] args) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();
        String ids = "4157,4158,4159,4160,4161,4162,4684,4685,4686,4687,4688,4689,4690,4691,4693,4694,4696,4697,4698,4699,4700,4701,4702,4703,4706,4708,4709,4710,4711,4712,4713,4714,4715,4716,4717,4718,4719,4720,4722,4723,4725,4726,4727,4728,4729,4730,4731,4732,4733,4734,4735,4766,4825,4830,4831,4945";
        String[] idsSep = ids.split(",");
        for (String id : idsSep) {
            HttpPost httppost = new HttpPost("http://79.143.20.s:8096/task/set/deadline/after/" + id);

            httppost.setHeader("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJrb2tlbm92YSIsImV4cCI6MTYzMjU5ODcwM30.7Kyi9ili1QvnqXIy6Buig2vH4RWyCiSvf1IvEiLywsBynLlwAnNM83Dl5ZPSoXWE8amgIOsr3DzjSxUZLNbM_g");

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");
            System.out.println(responseString);
        }

    }
}
