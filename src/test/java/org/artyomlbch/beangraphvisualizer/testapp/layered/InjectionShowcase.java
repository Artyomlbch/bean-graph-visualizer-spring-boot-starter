package org.artyomlbch.beangraphvisualizer.testapp.layered;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InjectionShowcase {

    private final CtorDependency ctorDependency;

    @Autowired
    private FieldDependency fieldDependency;

    private SetterDependency setterDependency;

    public InjectionShowcase(CtorDependency ctorDependency) {
        this.ctorDependency = ctorDependency;
    }

    @Autowired
    public void setSetterDependency(SetterDependency setterDependency) {
        this.setterDependency = setterDependency;
    }

}

@Component
class CtorDependency  {
}

@Component
class FieldDependency {
}

@Component
class SetterDependency {
}