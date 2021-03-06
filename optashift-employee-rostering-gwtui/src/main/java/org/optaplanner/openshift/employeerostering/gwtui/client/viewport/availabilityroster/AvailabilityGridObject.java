package org.optaplanner.openshift.employeerostering.gwtui.client.viewport.availabilityroster;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.MouseEvent;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.ForEvent;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.optaplanner.openshift.employeerostering.gwtui.client.common.FailureShownRestCallback;
import org.optaplanner.openshift.employeerostering.gwtui.client.viewport.grid.Lane;
import org.optaplanner.openshift.employeerostering.gwtui.client.viewport.grid.SingleGridObject;
import org.optaplanner.openshift.employeerostering.gwtui.client.viewport.impl.AbstractHasTimeslotGridObject;
import org.optaplanner.openshift.employeerostering.shared.common.HasTimeslot;
import org.optaplanner.openshift.employeerostering.shared.employee.Employee;
import org.optaplanner.openshift.employeerostering.shared.employee.EmployeeAvailabilityState;
import org.optaplanner.openshift.employeerostering.shared.employee.EmployeeRestServiceBuilder;
import org.optaplanner.openshift.employeerostering.shared.employee.view.EmployeeAvailabilityView;
import org.optaplanner.openshift.employeerostering.shared.roster.RosterState;

@Templated
public class AvailabilityGridObject extends AbstractHasTimeslotGridObject<AvailabilityRosterMetadata> implements SingleGridObject<LocalDateTime, AvailabilityRosterMetadata> {

    @Inject
    @DataField("label")
    @Named("span")
    private HTMLElement label;

    @Inject
    @DataField("timeslot-desired")
    private HTMLButtonElement timeslotDesiredButton;

    @Inject
    @DataField("timeslot-undesired")
    private HTMLButtonElement timeslotUndesiredButton;

    @Inject
    @DataField("timeslot-unavailable")
    private HTMLButtonElement timeslotUnavailableButton;

    @Inject
    private ManagedInstance<AvailabilityGridObjectPopup> popoverInstances;

    private EmployeeAvailabilityView employeeAvailabilityView;

    public AvailabilityGridObject withEmployeeAvailabilityView(EmployeeAvailabilityView employeeAvailabilityView) {
        this.employeeAvailabilityView = employeeAvailabilityView;
        refresh();
        return this;
    }

    @Override
    public LocalDateTime getStartPositionInScaleUnits() {
        return employeeAvailabilityView.getStartDateTime();
    }

    @Override
    public void setStartPositionInScaleUnits(LocalDateTime newStartPosition) {
        employeeAvailabilityView.setStartDateTime(newStartPosition);
    }

    @Override
    public LocalDateTime getEndPositionInScaleUnits() {
        return employeeAvailabilityView.getEndDateTime();
    }

    @Override
    public void setEndPositionInScaleUnits(LocalDateTime newEndPosition) {
        employeeAvailabilityView.setEndDateTime(newEndPosition);
    }

    @Override
    public Long getId() {
        return employeeAvailabilityView.getId();
    }

    @EventHandler("root")
    private void onClick(@ForEvent("click") MouseEvent e) {
        if (e.shiftKey) {
            getLane().removeGridObject(this);
        } else {
            popoverInstances.get().init(this);
        }
    }

    @EventHandler("delete")
    private void onDeleteClick(@ForEvent("click") MouseEvent e) {
        e.stopPropagation();
        EmployeeRestServiceBuilder.removeEmployeeAvailability(employeeAvailabilityView.getTenantId(), employeeAvailabilityView.getId(),
                                                              FailureShownRestCallback.onSuccess(success -> {
                                                                  getLane().removeGridObject(this);
                                                              }));
    }

    @EventHandler("timeslot-unavailable")
    private void onTimeslotUnavailableButtonClick(@ForEvent("click") MouseEvent e) {
        e.stopPropagation();
        setEmployeeAvailabilityState(EmployeeAvailabilityState.UNAVAILABLE);
    }

    @EventHandler("timeslot-undesired")
    private void onTimeslotUndesiredButtonClick(@ForEvent("click") MouseEvent e) {
        e.stopPropagation();
        setEmployeeAvailabilityState(EmployeeAvailabilityState.UNDESIRED);
    }

    @EventHandler("timeslot-desired")
    private void onTimeslotDesiredButtonClick(@ForEvent("click") MouseEvent e) {
        e.stopPropagation();
        setEmployeeAvailabilityState(EmployeeAvailabilityState.DESIRED);
    }

    private void setEmployeeAvailabilityState(EmployeeAvailabilityState state) {
        employeeAvailabilityView.setState(state);
        EmployeeRestServiceBuilder.updateEmployeeAvailability(employeeAvailabilityView.getTenantId(), employeeAvailabilityView,
                                                              FailureShownRestCallback.onSuccess(eav -> {
                                                                  withEmployeeAvailabilityView(eav);
                                                              }));
    }

    @Override
    protected HasTimeslot getTimeslot() {
        return employeeAvailabilityView;
    }

    private void refresh() {
        if (getLane() != null) {
            getLane().positionGridObject(this);
            label.innerHTML = new SafeHtmlBuilder().appendEscaped(employeeAvailabilityView.getState().toString()).toSafeHtml().asString();
            setClassProperty("desired", employeeAvailabilityView.getState() == EmployeeAvailabilityState.DESIRED);
            setClassProperty("undesired", employeeAvailabilityView.getState() == EmployeeAvailabilityState.UNDESIRED);
            setClassProperty("unavailable", employeeAvailabilityView.getState() == EmployeeAvailabilityState.UNAVAILABLE);
            timeslotDesiredButton.removeAttribute("active");
            timeslotUndesiredButton.removeAttribute("active");
            timeslotUnavailableButton.removeAttribute("active");

            switch (employeeAvailabilityView.getState()) {
                case UNAVAILABLE:
                    timeslotUnavailableButton.setAttribute("active", true);
                    break;

                case UNDESIRED:
                    timeslotUndesiredButton.setAttribute("active", true);
                    break;

                case DESIRED:
                    timeslotDesiredButton.setAttribute("active", true);
                    break;

                default:
                    throw new IllegalStateException("No case for " + employeeAvailabilityView.getState() + ".");
            }

            RosterState rosterState = getLane().getMetadata().getRosterState();
            setClassProperty("historic", rosterState.isHistoric(employeeAvailabilityView.getStartDateTime()));
            setClassProperty("published", rosterState.isPublished(employeeAvailabilityView.getStartDateTime()));
            setClassProperty("draft", rosterState.isDraft(employeeAvailabilityView.getStartDateTime()));
        }
    }

    @Override
    protected void init(Lane<LocalDateTime, AvailabilityRosterMetadata> lane) {
        refresh();
    }

    public EmployeeAvailabilityView getEmployeeAvailabilityView() {
        return employeeAvailabilityView;
    }

    public Employee getEmployee() {
        return getLane().getMetadata().getEmployeeIdToEmployeeMap().get(employeeAvailabilityView.getEmployeeId());
    }

    @Override
    public void save() {
        EmployeeRestServiceBuilder.updateEmployeeAvailability(employeeAvailabilityView.getTenantId(), employeeAvailabilityView,
                                                              FailureShownRestCallback.onSuccess(eav -> {
                                                                  withEmployeeAvailabilityView(eav);
                                                              }));
    }

}
