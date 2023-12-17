/* Nadav Horowitz CS320 Assignment1 2/8/2023
* This program uses Regular Expressions and Web Scraping to extract, format, and print data for the bus schedules of King County, WA.
* The program prints information about different bus routes like bus numbers and trip lengths.
*/
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class RouteFinder implements IRouteFinder {

    //Default constructor
    public RouteFinder(){}

    //runApp method is responsible for running the RouteFinder application. Takes user input for destination starting letter,
    //destination city, and responsible for continuing or quitting the program according to user input
    public void runApp(){
        boolean running = true;
        Scanner input = new Scanner(System.in);
        while(running){            
            System.out.println("Please enter a letter that your destinations start with");
            String inputLetterString = input.next();
            char inputLetter = inputLetterString.charAt(0);
            
            Map<String, Map<String, String>> busRoutesUrls = getBusRoutesUrls(inputLetter);
            printCityNamesAndBusNumbers(busRoutesUrls);

            System.out.println("Please enter your destination: ");
            String destination = input.next();

            printRouteTimes(busRoutesUrls, destination);

            System.out.println("Do you want to check different destination? Please type Y to continue or press any other key to exit");
            String continueProgram = input.next();
            if(!continueProgram.equals("Y")) {
                running = false;
            }
        }
        input.close();
    }


    //printRouteTimes method prints bus trip length information to console.
    //@param busRoutesUrls: key/value map containing bus information with key/value pair as destination and an inner map
    //with key/value pair of bus number and the routepage URL. destination: desired user destination to print bus information about
    public void printRouteTimes(Map<String, Map<String, String>> busRoutesUrls, String destination){
        System.out.println("Bus Trips Lengths in Minutes are:");
        Map<String,String> routeUrlMap = busRoutesUrls.get(destination);
        Map<String, List<Long>> busRouteTripsLengths = getBusRouteTripsLengthsInMinutesToAndFromDestination(routeUrlMap);
        System.out.println(busRouteTripsLengths.toString());
    }


    //printCityNamesAndBusNumbers method prints the names of cities and each of the bus route numbers within them.
    //@param busRoutesUrls: key/value map containing bus information with key/value pair as destination and an inner map
    //with key/value pair of bus number and the routepage URL.
    public void printCityNamesAndBusNumbers(Map<String, Map<String, String>> busRoutesUrls){
        Set<String> busRoutesUrlsKeySet = busRoutesUrls.keySet();
        Iterator<String> busRoutesUrlsKeySetIterator = busRoutesUrlsKeySet.iterator();
        
        while(busRoutesUrlsKeySetIterator.hasNext()){
            String cityName = busRoutesUrlsKeySetIterator.next();
            System.out.println("Destination: " + cityName);

            Map<String,String> routeUrlMap = busRoutesUrls.get(cityName);
            Set<String> routeUrlMapKeySet = routeUrlMap.keySet();
            Iterator<String> routeUrlMapKeySetIterator = routeUrlMapKeySet.iterator();

            while(routeUrlMapKeySetIterator.hasNext()){
                String route = routeUrlMapKeySetIterator.next();
                System.out.println("Bus Number: " + route);
            }
            System.out.println("+++++++++++++++++++++++++++++++++++");
        }
    }


    //getBusRouteTripsLengthsInMinutesToAndFromDestination returns list of trip lengths in minutes, grouped by bus route, destination,
    //and starting locations.
    //@param destinationBusesMap: key/value map with bus number as key and value as route page URL
    //@return key/value map of the trip lengths in minutes with key is the route ID - destination and value is the trip lengths in minutes
    public Map<String, List<Long>> getBusRouteTripsLengthsInMinutesToAndFromDestination(final Map<String, String> destinationBusesMap){
        Map<String, List<Long>> busRouteTripsLengths = new TreeMap<String, List<Long>>();
        
        Set<String> keySet = destinationBusesMap.keySet();
        Iterator<String> keySetIterator = keySet.iterator();
        while(keySetIterator.hasNext()){
            String route = keySetIterator.next();
            String urlString = destinationBusesMap.get(route);
            String websiteHTML = getUrlText(urlString);
            
            ArrayList<String> routeStarts = matchFinder("Weekday<small>To ([\\S\\s]*?)<.*Weekday<small>To ([\\S\\s]*?)</small>", websiteHTML);
            
            for(int i = 0; i < routeStarts.size(); i++){
                String routeNameAndNumber = route + " - " + routeStarts.get(i);
                if (routeNameAndNumber.contains("&amp;")){
                    routeNameAndNumber = routeNameAndNumber.replace("&amp;", "&");
                }
                ArrayList<Long> tripLengthList = new ArrayList<Long>();
                ArrayList<String> tableDataStrings = matchFinder("<h2>Weekday.*?</table>",websiteHTML);
                String tableData = tableDataStrings.get(i);

                ArrayList<String> tableRows = matchFinder("<tr>.*?</tr>",tableData);
                for(int j = 1; j < tableRows.size(); j++){
                    String tableRow = tableRows.get(j);
                    ArrayList<String> busStopTimes = matchFinder("\\d*:\\d\\d [AP]M",tableRow);
                    String startTime = busStopTimes.get(0);
                    String endTime = busStopTimes.get(busStopTimes.size() - 1);

                    long tripLength = tripLengthFinder(startTime,endTime);
                    tripLengthList.add(tripLength);
                }
                busRouteTripsLengths.put(routeNameAndNumber,tripLengthList);
            }
        }
        return busRouteTripsLengths;
    }


    //tripLengthFinder method is responsible for calculating the length of a bus trip when given a start time and an end time.
    //@param startTime: starting time of route. endTime: ending time of route
    //@return: returns the length of the trip in minutes
    public int tripLengthFinder(String startTime, String endTime){
        int minutesOfStartTime = timeToNumberOfMinutes(startTime);
        int minutesOfEndTime = timeToNumberOfMinutes(endTime);
        if(startTime.contains("PM") && endTime.contains("AM")){
            minutesOfStartTime = Math.abs(minutesOfStartTime - 1440);
            return(minutesOfStartTime + minutesOfEndTime);
        }
        else {
            return (minutesOfEndTime - minutesOfStartTime);
        }
    }


    //timeToNumberOfMinutes method converts a string containing a time value to the equivalent number of minutes past midnight
    //@parameter inputTime: string containing a time value
    //@return: equivalent number of minutes past midnight
    public int timeToNumberOfMinutes(String inputTime){
        int colonIndex = inputTime.indexOf(":");
        int hours = Integer.parseInt(inputTime.substring(0,colonIndex));
        int minutes = Integer.parseInt(inputTime.substring(colonIndex + 1,colonIndex + 3));
        if(inputTime.contains("AM")){
            if(hours == 12){
                hours = 0;
            }
        }
        else if(inputTime.contains("PM") && hours != 12){
            hours += 12;
        }
        minutes += (60* hours);
        return minutes;
    }


    //getUrlText method is responsible for scraping the raw HTML from the bus information website. 
    //Prints "Web Scraping Failed" when unable to scrape HTML.
    //@param urlString: contains url to be scraped.
    //@return: scraped HTML.
    private String getUrlText(String urlString){
        String scrapedHTML = "";
        try{
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            BufferedReader htmlReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = "";
            while ((inputLine = htmlReader.readLine()) != null) {
                scrapedHTML += inputLine;
            }
            htmlReader.close();
        } catch(Exception ex){
            System.out.println("Web Scraping Failed");
            System.exit(0);
        }
        return scrapedHTML;
    }


    //matchFinder method is responsible for finding and returning matches from inputted regex string and textInput string
    //@param regex: contains the regular expression specifying the wanted matches. textInput: contains the text to be searched for matches
    //@return: ArrayList<String> containing all the matches found according to inputted regex and textInput.
    public ArrayList<String> matchFinder(String regex, String textInput){
        int capturingGroups = countCapturingGroups(regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(textInput);
        ArrayList<String> matchList = new ArrayList<String>();

        while(matcher.find()){
            String match = "";
            if(capturingGroups == 0){
                match = matcher.group(0);
                matchList.add(match);
            }
            else if (capturingGroups > 0){
                for(int i = 1; i <= capturingGroups; i++){
                    match = matcher.group(i);
                    if(capturingGroups == 1 && i == 1){
                        match = matchFormatter(match);
                    }
                    matchList.add(match);

                }
            }
        }
        return matchList;
    }


    //matchFormatter method is responsible for adding & removing escape characters for parentheses
    //for correctly signifying caputring groups in a Regular Expression.
    //@param input: input string containing parentheses with or without excape characters
    //return: equivalent string with escape characters added or removed
    public String matchFormatter(String input){
        if(input.contains("\\(")){
            input = input.replace("\\(", "(");
            input = input.replace("\\)", ")");
        }
        else {
            input = input.replace("(", "\\(");
            input = input.replace(")", "\\)");
        }
        return input;
    }


    //countCapturingGroups method takes a string with a Regular Expression and returns the number of capturing groups within it
    //@param regex: string containing Regular Expression
    //@return: number of capturing groups in the Regular Expression string
    public int countCapturingGroups(String regex){
        int capturingGroups = 0;
        for(int i = 0; i < regex.length(); i++){
            char regexLetter = regex.charAt(i);
            if (regexLetter == '('){
                char escapeChecker = regex.charAt(i - 1);
                if(escapeChecker != '\\'){
                    capturingGroups++;
                }
            }
        }
        return capturingGroups;
    }


    //getBusRoutesUrls method returns a map containing information about bus routes in cities of interest.
    //throws RuntimeException if inputted destInitial is not a letter.
    //@param destInitial: first letter of cities of interest, inputted by user.
    //@return key/value map of the routes with key is destination and value is an inner map with a pair of route ID and the route page URL
    public Map<String, Map<String, String>> getBusRoutesUrls(final char destInitial){  
        if (!Character.isLetter(destInitial)){
            throw new RuntimeException();
        }
        char destInitialUppercase = Character.toUpperCase(destInitial);
        Map<String, Map<String, String>> cityRouteMap = new TreeMap<String, Map<String, String>>(); 

        String websiteHTML = getUrlText(TRANSIT_WEB_URL);
        String cityNameRegex = "<h3>(" + destInitialUppercase + "[\\s\\S]*?)</h3>";
        ArrayList<String> cityList = matchFinder(cityNameRegex, websiteHTML);
          
        for(int i = 0; i < cityList.size(); i++){
            String cityName = cityList.get(i);
            String cityDataRegex = cityName + "</h3>[\\s\\S]*?<h3>\\S|" + cityName + "</h3>[\\s\\S]*?<th>V";
            ArrayList<String> cityDataList = matchFinder(cityDataRegex, websiteHTML);

            for(int j = 0; j < cityDataList.size(); j++){
                Map<String, String> routeToUrlMap = new TreeMap<String, String>();
                String cityData = cityDataList.get(j);
                String routeDataRegex = "<a href=\"(\\S*?)\".*?>(.*?)</a>";
                ArrayList<String> urlAndRouteList = matchFinder(routeDataRegex, cityData);

                for(int k = 0; k < urlAndRouteList.size(); k = k + 2) {
                    String urlString = "https://www.communitytransit.org/busservice" + urlAndRouteList.get(k);
                    String routeNumber = urlAndRouteList.get(k + 1);
                    routeToUrlMap.put(routeNumber,urlString);
                }
                cityRouteMap.put(cityName,routeToUrlMap);
            }
        }
        return cityRouteMap;
    } 
}