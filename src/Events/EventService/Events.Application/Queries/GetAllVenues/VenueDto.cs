namespace EventService.Events.Application.Queries.GetAllVenues;

public class VenueDto
{
    public int VenueId { get; set; }
    public string VenueName { get; set; }
    public int CityId { get; set; }
    public string CityName { get; set; }
    public string Street { get; set; }
    public string PostalCode { get; set; }
    public decimal Latitude { get; set; }
    public decimal Longitude { get; set; }
}