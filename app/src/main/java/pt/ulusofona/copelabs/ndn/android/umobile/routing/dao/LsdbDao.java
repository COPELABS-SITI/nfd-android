package pt.ulusofona.copelabs.ndn.android.umobile.routing.dao;

import java.util.List;

import pt.ulusofona.copelabs.ndn.android.umobile.routing.exceptions.NeighborNotFoundException;
import pt.ulusofona.copelabs.ndn.android.umobile.routing.models.Plsa;

/**
 * Created by copelabs on 09/04/2018.
 */

public interface LsdbDao {
    void insertPlsa(Plsa plsa);
    void deletePlsa(Plsa plsa);
    void updatePlsa(Plsa plsa);
    boolean existsName(String name, String neighbor);
    List<Plsa> getAllEntries();
    Plsa getPlsa(String name, String neighbor) throws NeighborNotFoundException;
}