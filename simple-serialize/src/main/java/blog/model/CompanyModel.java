package blog.model;

import blog.serilize.base.*;

import java.util.Map;

public class CompanyModel implements DMarshallable {
    private int companyId;
    private Map<String, UserModel> info;



    public CompanyModel() {
    }

    public Map<String, UserModel> getInfo() {
        return info;
    }

    public void setInfo(Map<String, UserModel> info) {
        this.info = info;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeIntOptimise(companyId);
        marshaller.write(info,output);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.companyId = input.readIntPositiveOptimise();
        this.info = marshaller.read(input);
    }

    public static class CompanyInstanceImpl implements DInstance<CompanyModel>{

        @Override
        public CompanyModel instance() {
            return new CompanyModel();
        }
    }
}
