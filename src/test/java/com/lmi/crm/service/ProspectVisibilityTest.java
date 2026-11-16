package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.ProspectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProspectVisibilityTest {

    @Mock ProspectRepository prospectRepository;
    @Mock ProspectLicenseeRepository prospectLicenseeRepository;
    @Mock UserRepository userRepository;
    @Mock AlertRepository alertRepository;
    @Spy ProspectMapper prospectMapper = new ProspectMapper();
    @InjectMocks ProspectServiceImpl service;

    private static final int PROSPECT_ID = 68;
    private static final int OWNER_LICENSEE = 11;
    private static final int OTHER_LICENSEE = 12;
    private static final int OWNER_ASSOCIATE = 30;

    private User user(int id, UserRole role, String first, String last) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setFirstName(first);
        u.setLastName(last);
        return u;
    }

    private ProspectLicensee link(int licenseeId) {
        ProspectLicensee pl = new ProspectLicensee();
        pl.setProspectId(PROSPECT_ID);
        pl.setLicenseeId(licenseeId);
        pl.setIsPrimary(true);
        return pl;
    }

    @BeforeEach
    void setUp() {
        Prospect p = new Prospect();
        p.setId(PROSPECT_ID);
        p.setCompanyName("IITPL");
        p.setCity("Delhi");
        p.setContactFirstName("Gaurav");
        p.setContactLastName("Agarwal");
        p.setDesignation("MD");
        p.setEmail("gaurav@example.com");
        p.setPhone("9999999999");
        p.setType(ProspectType.PROSPECT);
        p.setStatus(ProspectStatus.PROTECTED);
        p.setAssociateId(OWNER_ASSOCIATE);
        p.setDeletionStatus(false);

        when(prospectRepository.findById(PROSPECT_ID)).thenReturn(Optional.of(p));
        when(prospectLicenseeRepository.findByProspectIdInAndIsPrimaryTrue(anyList())).thenReturn(List.of(link(OWNER_LICENSEE)));
        when(prospectLicenseeRepository.findByLicenseeId(OWNER_LICENSEE)).thenReturn(List.of(link(OWNER_LICENSEE)));
        when(prospectLicenseeRepository.findByLicenseeId(OTHER_LICENSEE)).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(user(OWNER_LICENSEE, UserRole.LICENSEE, "Asha", "Rao")));
    }

    private ProspectResponse viewAs(User requester) {
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        return service.getProspectDetail(requester.getId(), PROSPECT_ID);
    }

    @Test
    void otherLicenseeGetsCompanyOnlyViewWithOwner() {
        ProspectResponse r = viewAs(user(OTHER_LICENSEE, UserRole.LICENSEE, "B", "B"));

        assertTrue(r.isRestricted());
        assertEquals("IITPL", r.getCompanyName());
        assertEquals("Delhi", r.getCity());
        assertEquals(ProspectStatus.PROTECTED, r.getStatus());
        assertEquals(OWNER_LICENSEE, r.getLicenseeId());
        assertEquals("Asha Rao", r.getLicenseeName());
        assertNull(r.getContactFirstName());
        assertNull(r.getContactLastName());
        assertNull(r.getDesignation());
        assertNull(r.getEmail());
        assertNull(r.getPhone());
        assertNull(r.getAssociateId());
    }

    @Test
    void owningLicenseeGetsFullDetails() {
        ProspectResponse r = viewAs(user(OWNER_LICENSEE, UserRole.LICENSEE, "Asha", "Rao"));
        assertFalse(r.isRestricted());
        assertEquals("gaurav@example.com", r.getEmail());
        assertEquals("Asha Rao", r.getLicenseeName());
    }

    @Test
    void assignedAssociateGetsFullDetails() {
        ProspectResponse r = viewAs(user(OWNER_ASSOCIATE, UserRole.ASSOCIATE, "A", "A"));
        assertFalse(r.isRestricted());
        assertEquals("9999999999", r.getPhone());
    }

    @Test
    void otherAssociateGetsCompanyOnlyView() {
        ProspectResponse r = viewAs(user(31, UserRole.ASSOCIATE, "X", "Y"));
        assertTrue(r.isRestricted());
        assertNull(r.getEmail());
    }

    @Test
    void adminGetsFullDetails() {
        ProspectResponse r = viewAs(user(1, UserRole.ADMIN, "Ad", "Min"));
        assertFalse(r.isRestricted());
        assertEquals("gaurav@example.com", r.getEmail());
    }
}
