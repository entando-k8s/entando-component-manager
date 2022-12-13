package org.entando.kubernetes.stubhelper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentAttribute;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;

public class BundleStubHelper {

    public static final String BUNDLE_CODE = "my-component-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA;
    public static final String BUNDLE_NAME = "my-bundle-name";
    public static final String BUNDLE_DESCRIPTION = "desc";
    public static final BundleType BUNDLE_TYPE = BundleType.SYSTEM_LEVEL_BUNDLE;
    public static final String V1_0_0 = "v1.0.0";
    public static final String V1_1_0 = "v1.1.0";
    public static final String V1_2_0 = "v1.2.0";
    public static final List<String> TAG_LIST = List.of(V1_0_0, V1_1_0, V1_2_0);
    private static final String THUMBNAIL =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAOEAAADhCAMAAAAJbSJIAAAAsVBMV"
                    + "EUGL4f///8AoOAGLYYAKIQGKYMApOMAmNoDabMFPpL5+/0AmttWaqUFQZMDcroBlNYGN40EXqsEVaM/XKAAJIMnSZYAIIIAJYN3h7UADH"
                    + "2DkboDg8cAHYHs8PYAGYCrtM9sfrDEzOA6VZuLm8Pf4+6lsc8AFX+zvdcXOo3R1+fn6/M0TpZYaKEDe8BOZKJfcKeVocUFW6gACH4FSZo"
                    + "Bi8+/yN5GZKZ8i7dnea6bp8knQ5Cg7tn3AAAGDElEQVR4nO2dbVubPBSAwSRUa7XMUmgVsJS+AbP1rU+r//+HPXTVrZpACHQmmef+vHLl"
                    + "HklOTnKIhgEAAAAAAAAAAAAAAAAAAAAAAAAAAKAeSBzZTRZjcSaOLbvRIpC785Yo56dEdrMFwDfWiSjWJZbdbAHwjbAgGCoGGOpvSMAQD"
                    + "JUHDMFQfcAQDNXnOxhaXPQ2RFc/uLS0NjQQ4dLV27ACYKg/YKg/YKg/YKg/YKg/YKg/YCgPRDADInxIraQhwe4stPu9Zeczy/GrN/QdLO"
                    + "CpniHy/P4qjtIgaLMI0sntZmmHlSVVM0SuvY1Sk8dkvjTcao6KGfrePV/vTTIeulUeqZShF963K/rtaF+7FRqrkCFyOxMBvx3J0ud2VXU"
                    + "MkROLvMC317jhjkZlDFE4F/bbcc9TVMUQ+VEtQdOchuVPVsQQ4XVNwVxxVvpoNQyRc1tb0DQHpVHjo6HVPZVSuedfNxA023ZZLd6BoXWS"
                    + "+0npozgTn0UPmZfNNt2D9/dA5NQlokfROPiZjlf89DdDy3q+csQzk+PgbhoKmolX3PTuvn/ePMmrDkaLoKmhuSweXbmhdX73hCWWP/txY"
                    + "0Fz6hc+vmudXF6IJJRHBy2qZhMlBE7h858vDal+huEMWE1uR/Ey639mvBxNE9a82y7upkh6WXeYMFo8zxwXM46sseM72ZTxg+tKuaIUCC"
                    + "MWBqNZ8byOSMh46yUDUTYuYzmzKl9Mo5B+i2vO+lsijKQp5rUW2dRvkvLlt0yGVDBMuVMfcqixmw6/pLU1QAY9pPiThku9+EBZQ9yjR2H"
                    + "JIvMNrQxXlGGfH8BcKp1U19AbUYY2fwUChioBhmy+gaFGcykjs+AbIif61w0JtVpXd01Ty9CnfzTRydDwWAf473iOO+zQCZe6K2+G4Yo6"
                    + "vT9gtRrEzJRZ2eyJYcg8vv8DQy/nRdkcn71LI86geCtKMkcyDCos1yVxJEN1J5pjGY6U7aRHMpws1L2xxGl8KrOj/IxULqzNRGEiR91Xe"
                    + "BTDtvyN+xKOYBgs+VtXEmluGKzU3dDf0dgwHSs8y+xoajidya6H5dHMMOpVLDKVSAPDYN5xVH+BRn3D9nrzOpN8el0NccNgsr7dZLNQ5S"
                    + "h/CMPwv14hWX+BsOe60kp/asBYl14U79MQgpCh2b2A3pYy7GgwfQjAOD+8VzyEC8I4A47Uj3FC0Of45oXKqYI4IV3+XOEg/xdEj4Dh0nV"
                    + "77XGVuQa5WayFIu4xtuhLykXfIbOBaY50mJRQyCgQns84isjvR/m/CzIdIgujm+aKfulsQ/zBfoJKlD2uOIBkrBLh9aJkB9RZ/D4DjnXo"
                    + "pz7zc6B0+1jQA/Fwe/B/MlZ6k2YPyViGeQ/sDB2qryJvuPxwupYu1I+eiK6seHfcZKGL96vtHIK90N5Gn4esGkug0nyA0MWU7wRJ3Bm/G"
                    + "tjzMFq89kZzRk34Rv5eGyLk4alMsbxavz1JkihKkknBRwtt6SdrGP/oWldlrUAz1sF1ZSaPUr81wPZpy7LKDQ3MjBiVeZE3FBE+Oz3f3e"
                    + "vHMTRcOhEWYSXp+BCRs8vW/t5CniFirmwqk2YyhiLx7LuT93sZeYb58rTJN5bm6MvjPkLe1c3BvZNcQwM91vvSeS/45QEDoavnjx+ncg0"
                    + "N5LC+halCsPz6Ybj46FfJMF+Q1RuLSfb1gujivIZhnvTRZe18XrCEHLGmYR40+qKhfzIOZcTC2oYGdoWujkg3oaQLEmob5vNNVvnugTR+"
                    + "lbWYaWCYx9FwFVXzW/AvNvlbNDI0DG+25MbG9daXmRc2NMyHY+heR4UDMl2PwlDu5kVjw914xK/bOZUSBpMo7i0c6XV6RzDcrdu9kGQ/R"
                    + "/F0nqe/STR/uR79zOzQlfll/e/GHcPwFwQ7rh/OckLfd7waF9L9HY5nqCpgqD9gqD9gqD9gqD9gqD9gqD9gqD8Mwwebi+xWi0AbnrS6PF"
                    + "o6KTIM+VgVbotQBjAEQ/UBQzBUHzAEQ/UBQzBUHzD8Jwz5f02aQqf80LBPayC70WLw/5g0jew2AwAAAAAAAAAAAAAAAAAAAAAAAAAAfEv"
                    + "+By+Snrlln6cqAAAAAElFTkSuQmCC";

    public static EntandoDeBundle stubEntandoDeBundle() {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setLabels(Map.of("widgets", "true", "bundle-type", BUNDLE_TYPE.getType()));
        metadata.setName(BUNDLE_NAME);
        EntandoDeBundle entandoDeBundle = new EntandoDeBundle();
        entandoDeBundle.setMetadata(metadata);
        return entandoDeBundle;
    }


    public static BundleDescriptor stubBundleDescriptor(ComponentSpecDescriptor spec) {
        return stubBundleDescriptor(spec, BundleType.SYSTEM_LEVEL_BUNDLE);
    }

    public static BundleDescriptor stubBundleDescriptor(ComponentSpecDescriptor spec, BundleType type) {
        return new BundleDescriptor(BUNDLE_CODE, BUNDLE_NAME, BUNDLE_DESCRIPTION, type, spec, "{ \"test_ext\": true }",
                THUMBNAIL);
    }
}
